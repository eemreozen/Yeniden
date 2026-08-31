package com.yeniden.identity.otp;

import com.yeniden.identity.sms.SmsSender;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class OtpPropertiesTest {
    private final ApplicationContextRunner context = new ApplicationContextRunner()
            .withUserConfiguration(OtpConfiguration.class)
            .withBean(StringRedisTemplate.class, () -> mock(StringRedisTemplate.class))
            .withBean(SmsSender.class, () -> mock(SmsSender.class));

    static OtpProperties validProperties() {
        OtpProperties properties = new OtpProperties();
        properties.setSecret("test-only-secret-with-at-least-32-bytes");
        properties.setNamespace("test:identity:otp");
        return properties;
    }

    @Test
    void defaultsMatchSecurityPolicy() {
        OtpProperties properties = validProperties();
        properties.validate();
        assertThat(properties.getTtl()).isEqualTo(Duration.ofSeconds(180));
        assertThat(properties.getCooldown()).isEqualTo(Duration.ofSeconds(60));
        assertThat(properties.getMaxAttempts()).isEqualTo(5);
        assertThat(properties.getPhoneLimit()).isEqualTo(3);
        assertThat(properties.getIpLimit()).isEqualTo(10);
    }

    @Test
    void secretIsRequiredEvenLocally() {
        context.withPropertyValues("spring.profiles.active=local").run(c -> assertThat(c).hasFailed());
        OtpProperties properties = validProperties();
        properties.setSecret("x".repeat(31));
        assertThatThrownBy(properties::validate).isInstanceOf(IllegalStateException.class);
        properties.setSecret(" ".repeat(32));
        assertThatThrownBy(properties::validate).isInstanceOf(IllegalStateException.class);
        properties.setSecret("ş".repeat(16)); // Minimum is bytes, not Java character count.
        properties.validate();
    }

    @Test
    void namespaceIsRequiredOutsideLocalProfile() {
        context.withPropertyValues("identity.otp.secret=" + validProperties().getSecret())
                .run(c -> assertThat(c).hasFailed());
    }

    @Test
    void localProfileUsesAnIsolatedNamespace() {
        context.withPropertyValues("spring.profiles.active=local",
                        "identity.otp.secret=" + validProperties().getSecret())
                .run(c -> {
                    assertThat(c).hasNotFailed();
                    assertThat(c).hasSingleBean(OtpService.class);
                    assertThat(c.getBean(OtpProperties.class).getNamespace())
                            .isEqualTo("yeniden:identity:otp:local");
                });
    }

    @Test
    void bindsExplicitPolicyWithoutMakingRedisCalls() {
        context.withPropertyValues("identity.otp.secret=" + validProperties().getSecret(),
                        "identity.otp.namespace=test:explicit", "identity.otp.ttl=120s",
                        "identity.otp.cooldown=30s", "identity.otp.max-attempts=4",
                        "identity.otp.phone-limit=2", "identity.otp.ip-limit=8")
                .run(c -> {
                    assertThat(c).hasNotFailed();
                    OtpProperties properties = c.getBean(OtpProperties.class);
                    assertThat(properties.getTtl()).isEqualTo(Duration.ofSeconds(120));
                    assertThat(properties.getCooldown()).isEqualTo(Duration.ofSeconds(30));
                    assertThat(properties.getMaxAttempts()).isEqualTo(4);
                    assertThat(properties.getPhoneLimit()).isEqualTo(2);
                    assertThat(properties.getIpLimit()).isEqualTo(8);
                });
    }

    @Test
    void rejectsUnsafeLimitsDurationsAndClusterTags() {
        OtpProperties properties = validProperties();
        properties.setNamespace("unsafe{tag}");
        assertThatThrownBy(properties::validate).isInstanceOf(IllegalStateException.class);
        properties.setNamespace("valid");
        properties.setTtl(Duration.ZERO);
        assertThatThrownBy(properties::validate).isInstanceOf(IllegalStateException.class);
        properties.setTtl(Duration.ofSeconds(180));
        properties.setCooldown(Duration.ofMillis(-1));
        assertThatThrownBy(properties::validate).isInstanceOf(IllegalStateException.class);
        properties.setCooldown(Duration.ofSeconds(60));
        properties.setMaxAttempts(0);
        assertThatThrownBy(properties::validate).isInstanceOf(IllegalStateException.class);
        properties.setMaxAttempts(5);
        properties.setPhoneLimit(0);
        assertThatThrownBy(properties::validate).isInstanceOf(IllegalStateException.class);
        properties.setPhoneLimit(3);
        properties.setIpLimit(-1);
        assertThatThrownBy(properties::validate).isInstanceOf(IllegalStateException.class);
    }
}
