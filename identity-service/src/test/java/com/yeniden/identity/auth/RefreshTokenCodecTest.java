package com.yeniden.identity.auth;

import static org.assertj.core.api.Assertions.*;
import java.util.HashSet;
import org.junit.jupiter.api.Test;

class RefreshTokenCodecTest {
    private final RefreshTokenCodec codec = new RefreshTokenCodec();

    @Test
    void createsOpaque256BitCredentialsAndOnlyDeterministicHashes() {
        var seen = new HashSet<String>();
        for (int i = 0; i < 100; i++) {
            String raw = codec.generate();
            assertThat(java.util.Base64.getUrlDecoder().decode(raw)).hasSize(32);
            assertThat(seen.add(raw)).isTrue();
            assertThat(codec.hash(raw)).matches("[0-9a-f]{64}").isNotEqualTo(raw).isEqualTo(codec.hash(raw));
        }
    }

    @Test
    void rejectsMalformedCredentialsWithoutTrimmingOrLargeInputHashing() {
        for (String invalid : new String[]{null, "", " ", "x".repeat(42), "x".repeat(44), "+".repeat(43), "x".repeat(10000)}) {
            assertThatThrownBy(() -> codec.hash(invalid)).isInstanceOf(AuthException.class);
        }
    }

    @Test
    void responseLoggingDoesNotExposeCredentials() {
        assertThat(new TokenResponse("secret-access", "secret-refresh", true, 900).toString())
                .doesNotContain("secret-access", "secret-refresh");
    }
}
