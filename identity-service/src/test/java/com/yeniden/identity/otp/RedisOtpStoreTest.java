package com.yeniden.identity.otp;

import com.yeniden.common.exception.BaseException;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RedisOtpStoreTest {
    @Test
    void redisFailureDoesNotLeakKeysOrProofsThroughExceptionChains() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        when(redis.execute(any(RedisScript.class), anyList(), any(Object[].class)))
                .thenThrow(new IllegalStateException("sensitive Redis command details"));
        RedisOtpStore store = new RedisOtpStore(redis, OtpPropertiesTest.validProperties());
        assertThatThrownBy(() -> store.reserve("phone-hmac", "ip-hmac", UUID.randomUUID(), "proof"))
                .isInstanceOfSatisfying(BaseException.class, e -> {
                    assertThat(e.getHttpStatus()).isEqualTo(503);
                    assertThat(e.getErrorCode()).isEqualTo("service_unavailable");
                    assertThat(e.getCause()).isNull();
                    assertThat(e.getMessage()).doesNotContain("sensitive", "phone-hmac", "proof");
                });
    }

    @Test
    void nullScriptResultFailsClosed() {
        RedisOtpStore store = new RedisOtpStore(mock(StringRedisTemplate.class), OtpPropertiesTest.validProperties());
        assertThatThrownBy(() -> store.consume("phone-hmac", UUID.randomUUID(), "proof"))
                .isInstanceOfSatisfying(BaseException.class, e -> assertThat(e.getHttpStatus()).isEqualTo(503));
    }
}
