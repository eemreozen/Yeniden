package com.yeniden.identity.sms;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.Set;

/** Credentials deliberately have no generated toString; do not log or expose this bean. */
@ConfigurationProperties(prefix = "identity.sms-gate")
public class SmsGateProperties {
    private boolean enabled;
    private String baseUrl;
    private String username;
    private String password;
    private Duration connectTimeout = Duration.ofSeconds(3);
    // Overall exchange deadline, including connection and response body consumption.
    private Duration readTimeout = Duration.ofSeconds(5);
    private boolean allowPrivateHttp;
    /** Development-only opt-in; production must use an explicit recipient allowlist. */
    private boolean allowAllRecipients;
    private Set<String> allowedRecipients = Set.of();

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public Duration getConnectTimeout() { return connectTimeout; }
    public void setConnectTimeout(Duration connectTimeout) { this.connectTimeout = connectTimeout; }
    public Duration getReadTimeout() { return readTimeout; }
    public void setReadTimeout(Duration readTimeout) { this.readTimeout = readTimeout; }
    public boolean isAllowPrivateHttp() { return allowPrivateHttp; }
    public void setAllowPrivateHttp(boolean allowPrivateHttp) { this.allowPrivateHttp = allowPrivateHttp; }
    public boolean isAllowAllRecipients() { return allowAllRecipients; }
    public void setAllowAllRecipients(boolean allowAllRecipients) { this.allowAllRecipients = allowAllRecipients; }
    public Set<String> getAllowedRecipients() { return allowedRecipients; }
    public void setAllowedRecipients(Set<String> allowedRecipients) { this.allowedRecipients = allowedRecipients; }
}
