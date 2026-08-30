package com.yeniden.identity.otp;

import com.yeniden.common.exception.BaseException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PhoneNormalizerTest {
    @ParameterizedTest
    @ValueSource(strings = {"+905321234567", "905321234567", "05321234567", "5321234567",
            " +90 (532) 123-45-67 ", "(0532) 123 45 67", "532-123-4567"})
    void normalizesAllSupportedForms(String input) {
        assertThat(PhoneNormalizer.normalize(input)).isEqualTo("+905321234567");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "+15551234567", "+902121234567", "02121234567", "532123456",
            "53212345678", "00905321234567", "abc05321234567", "0532ext1234567", "++905321234567",
            "90+5321234567", "0532.123.4567", "0532/1234567", "０５３２１２３４５６７", "0532\n1234567"})
    void rejectsUnsupportedOrInvalidInputWithoutEchoingIt(String input) {
        assertThatThrownBy(() -> PhoneNormalizer.normalize(input))
                .isInstanceOfSatisfying(BaseException.class, e -> {
                    assertThat(e.getHttpStatus()).isEqualTo(400);
                    assertThat(e.getErrorCode()).isEqualTo("invalid_otp");
                    assertThat(e.getMessage()).doesNotContain("0532", "532123", "abc");
                });
    }
}
