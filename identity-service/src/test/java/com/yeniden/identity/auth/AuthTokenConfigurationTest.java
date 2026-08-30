package com.yeniden.identity.auth;

import com.nimbusds.jose.jwk.RSAKey;
import com.yeniden.identity.domain.User;
import com.yeniden.identity.domain.UserRole;
import com.yeniden.identity.domain.UserStatus;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.*;

import static org.assertj.core.api.Assertions.*;

class AuthTokenConfigurationTest {
    private static final Instant NOW = Instant.parse("2026-08-30T12:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final AuthTokenConfiguration CONFIG = new AuthTokenConfiguration();
    private static TokenProperties properties;
    private static RSAKey key;
    private static JwtEncoder encoder;
    private static JwtDecoder decoder;

    @BeforeAll
    static void initialize() throws Exception {
        properties = new TokenProperties();
        MockEnvironment local = new MockEnvironment();
        local.setActiveProfiles("local");
        key = CONFIG.authRsaKey(properties, local);
        encoder = CONFIG.jwtEncoder(key);
        decoder = CONFIG.jwtDecoder(key, properties, CLOCK);
    }

    @Test
    void localUsesDistinctIssuerAnd3072BitEphemeralKey() throws Exception {
        assertThat(properties.getIssuer()).isEqualTo(TokenProperties.LOCAL_ISSUER);
        assertThat(key.toRSAPublicKey().getModulus().bitLength()).isEqualTo(3072);
    }

    @Test
    void configuredAccessTokensCarryRequiredClaimsAndNoPersonalData() {
        User user = User.builder().id(UUID.randomUUID()).phone("+905551234567").email("private@example.com")
                .status(UserStatus.ACTIVE).roles(Set.of(UserRole.USER, UserRole.MODERATOR))
                .neighborhoodId(UUID.randomUUID()).build();
        AuthSession session = new AuthSession(UUID.randomUUID(), user.getId(), NOW, NOW.plus(Duration.ofDays(30)));
        String access = new AccessTokenIssuer(encoder, properties).issue(user, session, NOW, NOW.plusSeconds(900));
        Jwt jwt = decoder.decode(access);
        assertThat(jwt.getSubject()).isEqualTo(user.getId().toString());
        assertThat(jwt.getClaimAsString("sid")).isEqualTo(session.getId().toString());
        assertThat(jwt.getClaimAsString("nbh")).isEqualTo(user.getNeighborhoodId().toString());
        assertThat(jwt.getClaimAsStringList("roles")).containsExactly("MODERATOR", "USER");
        assertThat(jwt.getClaims()).doesNotContainKeys("phone", "email", "status", "balance", "trustScore");
        assertThat(jwt.getHeaders()).containsEntry("alg", "RS256");
        assertThat(jwt.getIssuedAt()).isEqualTo(NOW);
        assertThat(jwt.getExpiresAt()).isEqualTo(NOW.plusSeconds(900));
    }

    @Test
    void absentNeighborhoodIsOmitted() {
        User user = User.builder().id(UUID.randomUUID()).roles(Set.of(UserRole.USER)).build();
        AuthSession session = new AuthSession(UUID.randomUUID(), user.getId(), NOW, NOW.plusSeconds(900));
        assertThat(decoder.decode(new AccessTokenIssuer(encoder, properties)
                .issue(user, session, NOW, NOW.plusSeconds(900))).getClaims()).doesNotContainKey("nbh");
    }

    @Test
    void emptyRolesFailClosed() {
        User user = User.builder().id(UUID.randomUUID()).roles(Set.of()).build();
        AuthSession session = new AuthSession(UUID.randomUUID(), user.getId(), NOW, NOW.plusSeconds(900));
        assertThatThrownBy(() -> new AccessTokenIssuer(encoder, properties).issue(user, session, NOW, NOW.plusSeconds(900)))
                .isInstanceOf(AuthException.class);
    }

    @Test
    void invalidIssuerAudienceExpiryOrIdentityClaimsAreRejected() {
        List<JwtClaimsSet> invalid = List.of(
                claims().issuer("https://other.example").build(),
                claims().audience(List.of("other-api")).build(),
                claims().issuedAt(NOW.minusSeconds(900)).expiresAt(NOW).build(),
                claims().issuedAt(NOW.plusSeconds(1)).build(),
                claims().expiresAt(NOW.plusSeconds(901)).build(),
                claims().subject("not-a-uuid").build(),
                claims().claim("sid", "not-a-uuid").build(),
                claims().claim("roles", List.of("SUPERUSER")).build(),
                claims().claim("roles", List.of()).build(),
                claims().claim("nbh", "invalid").build(),
                claims().claims(map -> map.remove("exp")).build(),
                claims().claims(map -> map.remove("iat")).build(),
                claims().claims(map -> map.remove("sid")).build());
        invalid.forEach(value -> assertThatThrownBy(() -> decoder.decode(sign(value, SignatureAlgorithm.RS256)))
                .isInstanceOf(JwtException.class));
    }

    @Test
    void otherRsaAlgorithmAndWrongSigningKeyAreRejected() throws Exception {
        assertThatThrownBy(() -> decoder.decode(sign(claims().build(), SignatureAlgorithm.RS512)))
                .isInstanceOf(JwtException.class);
        MockEnvironment local = new MockEnvironment();
        local.setActiveProfiles("local");
        JwtEncoder otherEncoder = CONFIG.jwtEncoder(CONFIG.authRsaKey(new TokenProperties(), local));
        String other = otherEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(SignatureAlgorithm.RS256).build(), claims().build())).getTokenValue();
        assertThatThrownBy(() -> decoder.decode(other)).isInstanceOf(JwtException.class);
    }

    @Test
    void missingExternalKeysAndImplicitLocalProfileFailClosed() {
        TokenProperties outsideLocal = new TokenProperties();
        outsideLocal.setIssuer("https://identity.example");
        assertThatThrownBy(() -> CONFIG.authRsaKey(outsideLocal, new MockEnvironment())).isInstanceOf(IllegalStateException.class);
        MockEnvironment defaultsOnly = new MockEnvironment();
        defaultsOnly.setDefaultProfiles("local");
        assertThatThrownBy(() -> CONFIG.authRsaKey(new TokenProperties(), defaultsOnly)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void mixedProductionAndLocalProfilesAreForbidden() {
        for (String profile : List.of("prod", "production")) {
            MockEnvironment mixed = new MockEnvironment();
            mixed.setActiveProfiles("local", profile);
            assertThatThrownBy(() -> CONFIG.authRsaKey(new TokenProperties(), mixed)).isInstanceOf(IllegalStateException.class);
        }
    }

    @Test
    void lifetimeAndIssuerMisconfigurationAreRejected() {
        TokenProperties invalid = new TokenProperties();
        invalid.setIssuer("https://production.example");
        assertThatThrownBy(() -> invalid.validate(true)).isInstanceOf(IllegalStateException.class);
        invalid.setIssuer(TokenProperties.LOCAL_ISSUER);
        for (Duration duration : List.of(Duration.ZERO, Duration.ofSeconds(-1), Duration.ofMinutes(16), Duration.ofMillis(1500))) {
            invalid.setAccessTtl(duration);
            assertThatThrownBy(() -> invalid.validate(true)).isInstanceOf(IllegalStateException.class);
        }
        invalid.setAccessTtl(Duration.ofMinutes(15));
        invalid.setRefreshTtl(Duration.ofDays(31));
        assertThatThrownBy(() -> invalid.validate(true)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void productionLoadsExternalPkcs8AndX509PemAndRejectsLocalTokens(@TempDir Path directory) throws Exception {
        KeyPair pair = pair(2048);
        TokenProperties production = pemProperties(directory, pair);
        RSAKey loaded = CONFIG.authRsaKey(production, new MockEnvironment());
        JwtDecoder productionDecoder = CONFIG.jwtDecoder(loaded, production, CLOCK);
        JwtEncoder productionEncoder = CONFIG.jwtEncoder(loaded);
        String access = productionEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(SignatureAlgorithm.RS256).build(),
                claims().issuer(production.getIssuer()).build())).getTokenValue();
        assertThat(productionDecoder.decode(access).getIssuer().toString()).isEqualTo(production.getIssuer());
        assertThatThrownBy(() -> productionDecoder.decode(sign(claims().build(), SignatureAlgorithm.RS256))).isInstanceOf(JwtException.class);
    }

    @Test
    void weakOrMismatchedPemPairFailsAtStartup(@TempDir Path directory) throws Exception {
        TokenProperties weak = pemProperties(directory, pair(1024));
        assertThatThrownBy(() -> CONFIG.authRsaKey(weak, new MockEnvironment())).isInstanceOf(IllegalStateException.class);
        TokenProperties mismatch = pemProperties(directory, pair(2048));
        writePem(mismatch.getPublicKeyPath(), "PUBLIC KEY", pair(2048).getPublic().getEncoded());
        assertThatThrownBy(() -> CONFIG.authRsaKey(mismatch, new MockEnvironment())).isInstanceOf(IllegalStateException.class);
    }

    private static JwtClaimsSet.Builder claims() {
        return JwtClaimsSet.builder().issuer(properties.getIssuer()).audience(List.of(properties.getAudience()))
                .subject(UUID.randomUUID().toString()).claim("sid", UUID.randomUUID().toString())
                .claim("roles", List.of("USER")).issuedAt(NOW).expiresAt(NOW.plusSeconds(900));
    }

    private static String sign(JwtClaimsSet claims, SignatureAlgorithm algorithm) {
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(algorithm).build(), claims)).getTokenValue();
    }

    private static KeyPair pair(int bits) throws Exception {
        var generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(bits);
        return generator.generateKeyPair();
    }

    private static TokenProperties pemProperties(Path directory, KeyPair pair) throws Exception {
        TokenProperties value = new TokenProperties();
        value.setIssuer("https://identity.example");
        value.setPrivateKeyPath(directory.resolve("private.pem"));
        value.setPublicKeyPath(directory.resolve("public.pem"));
        writePem(value.getPrivateKeyPath(), "PRIVATE KEY", pair.getPrivate().getEncoded());
        writePem(value.getPublicKeyPath(), "PUBLIC KEY", pair.getPublic().getEncoded());
        return value;
    }

    private static void writePem(Path path, String label, byte[] der) throws Exception {
        Files.writeString(path, "-----BEGIN " + label + "-----\n" + Base64.getMimeEncoder().encodeToString(der)
                + "\n-----END " + label + "-----\n", StandardCharsets.US_ASCII);
    }
}
