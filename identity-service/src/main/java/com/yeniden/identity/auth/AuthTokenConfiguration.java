package com.yeniden.identity.auth;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Clock;
import java.time.Duration;
import java.util.Arrays;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.*;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(TokenProperties.class)
public class AuthTokenConfiguration {
    @Bean("authClock")
    public Clock authClock() { return Clock.systemUTC(); }

    @Bean
    public RSAKey authRsaKey(TokenProperties properties, Environment environment)
            throws GeneralSecurityException, IOException {
        var profiles = Arrays.asList(environment.getActiveProfiles());
        boolean local = profiles.contains("local");
        if (local && (profiles.contains("prod") || profiles.contains("production"))) {
            throw new IllegalStateException("local cannot be combined with a production profile");
        }
        properties.validate(local);
        RSAPublicKey publicKey;
        RSAPrivateKey privateKey;
        if (properties.getPrivateKeyPath() == null) {
            // This branch is reachable ONLY through an explicitly active local profile.
            var generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(3072);
            var pair = generator.generateKeyPair();
            publicKey = (RSAPublicKey) pair.getPublic();
            privateKey = (RSAPrivateKey) pair.getPrivate();
        } else {
            KeyFactory factory = KeyFactory.getInstance("RSA");
            publicKey = (RSAPublicKey) factory.generatePublic(new X509EncodedKeySpec(
                    readPem(properties.getPublicKeyPath(), "PUBLIC KEY")));
            privateKey = (RSAPrivateKey) factory.generatePrivate(new PKCS8EncodedKeySpec(
                    readPem(properties.getPrivateKeyPath(), "PRIVATE KEY")));
        }
        if (publicKey.getModulus().bitLength() < 2048 || !publicKey.getModulus().equals(privateKey.getModulus())) {
            throw new IllegalStateException("RSA keys must match and contain at least 2048 bits");
        }
        // Check the actual pair, not just its public modulus, before accepting external keys.
        Signature probe = Signature.getInstance("SHA256withRSA");
        byte[] message = "yeniden-key-pair-check".getBytes(StandardCharsets.US_ASCII);
        probe.initSign(privateKey);
        probe.update(message);
        byte[] signed = probe.sign();
        probe.initVerify(publicKey);
        probe.update(message);
        if (!probe.verify(signed)) throw new IllegalStateException("RSA private/public key pair does not match");
        RSAKey key = new RSAKey.Builder(publicKey).privateKey(privateKey).build();
        try {
            return new RSAKey.Builder(key).keyID(key.toPublicJWK().computeThumbprint().toString()).build();
        } catch (com.nimbusds.jose.JOSEException e) {
            throw new IllegalStateException("Cannot calculate RSA key identifier", e);
        }
    }

    @Bean
    public JwtEncoder jwtEncoder(RSAKey authRsaKey) {
        return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(authRsaKey)));
    }

    @Bean
    public JwtDecoder jwtDecoder(RSAKey authRsaKey, TokenProperties properties,
                                 @Qualifier("authClock") Clock clock) throws com.nimbusds.jose.JOSEException {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey(authRsaKey.toRSAPublicKey())
                .signatureAlgorithm(SignatureAlgorithm.RS256).build();
        JwtTimestampValidator timestamps = new JwtTimestampValidator(Duration.ZERO);
        timestamps.setClock(clock);
        OAuth2TokenValidator<Jwt> claims = jwt -> {
            try {
                java.util.UUID.fromString(jwt.getSubject());
                java.util.UUID.fromString(jwt.getClaimAsString("sid"));
                var issued = jwt.getIssuedAt();
                var expires = jwt.getExpiresAt();
                var roles = jwt.getClaimAsStringList("roles");
                boolean valid = issued != null && expires != null && expires.isAfter(issued)
                        && !issued.isAfter(clock.instant()) && expires.isAfter(clock.instant())
                        && Duration.between(issued, expires).compareTo(properties.getAccessTtl()) <= 0
                        && jwt.getAudience().contains(properties.getAudience())
                        && roles != null && !roles.isEmpty()
                        && roles.stream().allMatch(role -> "USER".equals(role) || "MODERATOR".equals(role) || "ADMIN".equals(role));
                if (jwt.hasClaim("nbh")) java.util.UUID.fromString(jwt.getClaimAsString("nbh"));
                if (valid) return OAuth2TokenValidatorResult.success();
            } catch (IllegalArgumentException | NullPointerException e) {
                // Malformed claims are authentication failures, never server errors.
            }
            return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Invalid access token claims", null));
        };
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(timestamps,
                new JwtIssuerValidator(properties.getIssuer()), claims));
        return decoder;
    }

    private static byte[] readPem(Path path, String label) throws IOException {
        String pem = Files.readString(path, StandardCharsets.US_ASCII).trim();
        String begin = "-----BEGIN " + label + "-----";
        String end = "-----END " + label + "-----";
        if (!pem.startsWith(begin) || !pem.endsWith(end)) {
            throw new IllegalStateException("Expected " + label + " PEM (PKCS#8 private / X.509 public)");
        }
        return Base64.getDecoder().decode(pem.substring(begin.length(), pem.length() - end.length()).replaceAll("\\s", ""));
    }
}
