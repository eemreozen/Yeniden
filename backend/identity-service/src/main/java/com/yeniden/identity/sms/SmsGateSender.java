package com.yeniden.identity.sms;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yeniden.common.exception.BaseException;

import java.net.InetAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * SMSGate Local Server: POST /message with Basic authentication.
 * https://docs.sms-gate.app/getting-started/local-server/
 * https://docs.sms-gate.app/features/sending-messages/
 * No application retries: timeout means acceptance is unknown. Reuse the challenge ID
 * if the calling workflow retries; do not interpret acceptance as handset delivery.
 */
public final class SmsGateSender implements SmsSender, AutoCloseable {
    private static final ObjectMapper JSON = new ObjectMapper();
    private final HttpClient client;
    private final URI endpoint;
    private final String authorization;
    private final Duration readTimeout;
    private final Set<String> allowedRecipients;
    private final boolean allowAllRecipients;

    public SmsGateSender(SmsGateProperties properties) {
        if (!properties.isEnabled()) {
            client = null;
            endpoint = null;
            authorization = null;
            readTimeout = null;
            allowedRecipients = Set.of();
            allowAllRecipients = false;
            return;
        }
        try {
            endpoint = messageEndpoint(properties.getBaseUrl(), properties.isAllowPrivateHttp());
            require(validTimeout(properties.getConnectTimeout()) && validTimeout(properties.getReadTimeout()));
            require(validCredential(properties.getUsername()) && !properties.getUsername().contains(":"));
            require(validCredential(properties.getPassword()));
            require(properties.getAllowedRecipients() != null);
            allowedRecipients = Set.copyOf(properties.getAllowedRecipients());
            allowAllRecipients = properties.isAllowAllRecipients();
            require(allowedRecipients.stream().allMatch(SmsGateSender::validPhone));
            authorization = "Basic " + Base64.getEncoder().encodeToString(
                    (properties.getUsername() + ":" + properties.getPassword()).getBytes(StandardCharsets.UTF_8));
            readTimeout = properties.getReadTimeout();
            client = HttpClient.newBuilder()
                    .connectTimeout(properties.getConnectTimeout())
                    .followRedirects(HttpClient.Redirect.NEVER)
                    // Do not route Basic credentials through a JVM/system HTTP proxy.
                    .proxy(ProxySelector.of(null))
                    .build();
        } catch (RuntimeException exception) {
            // Do not retain exceptions that may embed credentials, URI or other configuration values.
            throw unavailable();
        }
    }

    @Override
    public void send(String phone, String code, UUID challengeId) {
        if (client == null || Thread.currentThread().isInterrupted()
                || !validPhone(phone) || (!allowAllRecipients && !allowedRecipients.contains(phone))
                || code == null || !code.matches("[0-9]{6}") || challengeId == null) {
            throw unavailable();
        }
        CompletableFuture<HttpResponse<Void>> pending = null;
        try {
            byte[] body = JSON.writeValueAsBytes(Map.of(
                    "id", challengeId.toString(),
                    "textMessage", Map.of("text", "Yeniden dogrulama kodunuz: " + code),
                    "phoneNumbers", List.of(phone)));
            HttpRequest request = HttpRequest.newBuilder(endpoint)
                    .timeout(readTimeout)
                    .header("Authorization", authorization)
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                    .build();
            // Discard rather than buffer/log untrusted provider response bodies. The explicit
            // deadline also covers a server that sends headers then stalls its response body.
            pending = client.sendAsync(request, HttpResponse.BodyHandlers.discarding());
            int status = pending.get(readTimeout.toNanos(), TimeUnit.NANOSECONDS).statusCode();
            if (status < 200 || status >= 300) {
                throw unavailable();
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw unavailable();
        } catch (JsonProcessingException | ExecutionException | TimeoutException | RuntimeException exception) {
            throw unavailable();
        } finally {
            if (pending != null && !pending.isDone()) {
                pending.cancel(true);
            }
        }
    }

    /** Only an origin is accepted; credentials, paths, queries and fragments are forbidden. */
    static URI messageEndpoint(String baseUrl, boolean allowPrivateHttp) {
        try {
            URI base = URI.create(baseUrl);
            require(base.getHost() != null && base.getRawUserInfo() == null
                    && base.getRawQuery() == null && base.getRawFragment() == null
                    && (base.getRawPath().isEmpty() || base.getRawPath().equals("/"))
                    && (base.getPort() == -1 || base.getPort() >= 1 && base.getPort() <= 65535));
            require("https".equalsIgnoreCase(base.getScheme())
                    || "http".equalsIgnoreCase(base.getScheme()) && allowPrivateHttp && privateLiteral(base.getHost()));
            return base.resolve("/message");
        } catch (RuntimeException exception) {
            throw unavailable();
        }
    }

    private static boolean privateLiteral(String host) {
        // DNS names (even localhost) are deliberately excluded from plaintext transport:
        // validation followed by DNS resolution would permit DNS rebinding/secret leakage.
        if (host.matches("[0-9]+\\.[0-9]+\\.[0-9]+\\.[0-9]+")) {
            String[] parts = host.split("\\.");
            int[] octets = new int[4];
            for (int i = 0; i < 4; i++) {
                if (parts[i].length() > 3 || parts[i].length() > 1 && parts[i].startsWith("0")) {
                    return false;
                }
                octets[i] = Integer.parseInt(parts[i]);
                if (octets[i] > 255) {
                    return false;
                }
            }
            return octets[0] == 10 || octets[0] == 127
                    || octets[0] == 172 && octets[1] >= 16 && octets[1] <= 31
                    || octets[0] == 192 && octets[1] == 168;
        }
        if (host.startsWith("[") && host.endsWith("]")) {
            String literal = host.substring(1, host.length() - 1);
            if (!literal.contains(":") || !literal.matches("[0-9a-fA-F:]+")) {
                return false;
            }
            try {
                InetAddress address = InetAddress.getByName(literal); // validated numeric IPv6 only
                byte[] bytes = address.getAddress();
                return bytes.length == 16 && (address.isLoopbackAddress() || (bytes[0] & 0xfe) == 0xfc);
            } catch (java.net.UnknownHostException exception) {
                return false;
            }
        }
        return false;
    }

    private static boolean validTimeout(Duration timeout) {
        return timeout != null && timeout.compareTo(Duration.ofMillis(1)) >= 0
                && timeout.compareTo(Duration.ofSeconds(60)) <= 0;
    }

    private static boolean validPhone(String phone) {
        return phone != null && phone.matches("\\+[1-9][0-9]{1,14}");
    }

    private static boolean validCredential(String value) {
        return value != null && !value.isBlank() && value.chars().noneMatch(Character::isISOControl);
    }

    private static void require(boolean condition) {
        if (!condition) {
            throw unavailable();
        }
    }

    private static BaseException unavailable() {
        return new BaseException("SMS service is unavailable.", "service_unavailable", 503);
    }

    @Override
    public void close() {
        if (client != null) {
            client.shutdownNow();
        }
    }
}
