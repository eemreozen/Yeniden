package com.yeniden.identity.otp;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

/** Required credentials intentionally have no toString implementation. */
@ConfigurationProperties(prefix = "identity.otp")
public class OtpProperties {
    private String secret;
    private String namespace;
    private Duration ttl = Duration.ofSeconds(180);
    private Duration cooldown = Duration.ofSeconds(60);
    private int maxAttempts = 5;
    private int phoneLimit = 3;
    private int ipLimit = 10;

    void validate() {
        if (secret == null || secret.isBlank() || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("identity.otp.secret must contain at least 32 UTF-8 bytes");
        }
        if (namespace == null || !namespace.matches("[A-Za-z0-9:_-]{1,80}")) {
            throw new IllegalStateException("identity.otp.namespace is required (1-80 letters, digits, :, _, -)");
        }
        requireDuration(ttl, "ttl");
        requireDuration(cooldown, "cooldown");
        if (maxAttempts < 1 || phoneLimit < 1 || ipLimit < 1) {
            throw new IllegalStateException("identity.otp limits and max-attempts must be positive");
        }
    }

    private static void requireDuration(Duration value, String name) {
        if (value == null || value.compareTo(Duration.ofMillis(1)) < 0
                || value.compareTo(Duration.ofHours(1)) > 0) {
            throw new IllegalStateException("identity.otp." + name + " must be between 1ms and 1h");
        }
    }

    public String getSecret() { return secret; }
    public void setSecret(String secret) { this.secret = secret; }
    public String getNamespace() { return namespace; }
    public void setNamespace(String namespace) { this.namespace = namespace; }
    public Duration getTtl() { return ttl; }
    public void setTtl(Duration ttl) { this.ttl = ttl; }
    public Duration getCooldown() { return cooldown; }
    public void setCooldown(Duration cooldown) { this.cooldown = cooldown; }
    public int getMaxAttempts() { return maxAttempts; }
    public void setMaxAttempts(int maxAttempts) { this.maxAttempts = maxAttempts; }
    public int getPhoneLimit() { return phoneLimit; }
    public void setPhoneLimit(int phoneLimit) { this.phoneLimit = phoneLimit; }
    public int getIpLimit() { return ipLimit; }
    public void setIpLimit(int ipLimit) { this.ipLimit = ipLimit; }
}
