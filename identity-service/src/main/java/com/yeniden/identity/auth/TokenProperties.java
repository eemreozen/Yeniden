package com.yeniden.identity.auth;

import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "identity.tokens")
public class TokenProperties {
    public static final String LOCAL_ISSUER = "urn:yeniden:identity:local";
    private String issuer;
    private String audience = "yeniden-api";
    private Path privateKeyPath;
    private Path publicKeyPath;
    private Duration accessTtl = Duration.ofMinutes(15);
    private Duration refreshTtl = Duration.ofDays(30);

    void validate(boolean local) {
        if (local && (issuer == null || issuer.isBlank())) issuer = LOCAL_ISSUER;
        if (issuer == null || issuer.isBlank()) throw new IllegalStateException("identity.tokens.issuer is required");
        if (local && !LOCAL_ISSUER.equals(issuer)) {
            throw new IllegalStateException("local profile requires the isolated local issuer");
        }
        if (!local) {
            URI uri = URI.create(issuer);
            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null
                    || uri.getUserInfo() != null || uri.getQuery() != null || uri.getFragment() != null) {
                throw new IllegalStateException("identity.tokens.issuer must be an explicit HTTPS issuer outside local");
            }
            if (privateKeyPath == null || publicKeyPath == null) {
                throw new IllegalStateException("External RSA PEM key paths are required outside local");
            }
        }
        if (audience == null || audience.isBlank() || !audience.equals(audience.trim())) {
            throw new IllegalStateException("identity.tokens.audience must not be blank or padded");
        }
        validateTtl(accessTtl, Duration.ofMinutes(15), "access-ttl");
        validateTtl(refreshTtl, Duration.ofDays(30), "refresh-ttl");
        if (refreshTtl.compareTo(accessTtl) < 0) throw new IllegalStateException("refresh-ttl must cover access-ttl");
        if ((privateKeyPath == null) != (publicKeyPath == null)) {
            throw new IllegalStateException("Both RSA PEM key paths must be supplied together");
        }
    }

    private static void validateTtl(Duration value, Duration maximum, String name) {
        if (value == null || value.compareTo(Duration.ofSeconds(1)) < 0 || value.compareTo(maximum) > 0
                || value.getNano() != 0) {
            throw new IllegalStateException("identity.tokens." + name + " must be whole positive seconds, at most " + maximum);
        }
    }
}
