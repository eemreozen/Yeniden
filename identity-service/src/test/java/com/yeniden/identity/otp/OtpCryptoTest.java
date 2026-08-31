package com.yeniden.identity.otp;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class OtpCryptoTest {
    @Test
    void generatedCodesAreSixAsciiDigits() {
        OtpCrypto crypto = new OtpCrypto(OtpPropertiesTest.validProperties());
        for (int i = 0; i < 100; i++) assertThat(crypto.newCode()).matches("[0-9]{6}");
    }

    @Test
    void proofsBindSecretNamespacePhoneChallengeAndCodeWithSeparateKeyPurposes() {
        OtpProperties properties = OtpPropertiesTest.validProperties();
        OtpCrypto crypto = new OtpCrypto(properties);
        UUID id = UUID.randomUUID();
        String phone = "+905321234567";
        String proof = crypto.proof(phone, "012345", id);
        assertThat(proof).matches("[0-9a-f]{64}");
        assertThat(proof).isEqualTo(crypto.proof(phone, "012345", id));
        assertThat(proof).isNotEqualTo(crypto.proof(phone, "012346", id));
        assertThat(proof).isNotEqualTo(crypto.proof(phone, "012345", UUID.randomUUID()));
        assertThat(proof).isNotEqualTo(crypto.proof("+905321234568", "012345", id));
        assertThat(crypto.phoneKey(phone)).isNotEqualTo(crypto.ipKey(phone)).doesNotContain(phone);
        properties.setSecret("another-test-only-secret-of-at-least-32-bytes");
        OtpCrypto otherSecret = new OtpCrypto(properties);
        assertThat(proof).isNotEqualTo(otherSecret.proof(phone, "012345", id));
        assertThat(crypto.phoneKey(phone)).isNotEqualTo(otherSecret.phoneKey(phone));
        properties = OtpPropertiesTest.validProperties();
        properties.setNamespace("different:namespace");
        assertThat(proof).isNotEqualTo(new OtpCrypto(properties).proof(phone, "012345", id));
    }
}
