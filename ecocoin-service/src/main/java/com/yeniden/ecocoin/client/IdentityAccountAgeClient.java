package com.yeniden.ecocoin.client;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class IdentityAccountAgeClient {
    private final RestClient restClient;
    private final ConcurrentHashMap<UUID, Instant> cache = new ConcurrentHashMap<>();

    public IdentityAccountAgeClient(@Value("${identity.base-url:http://localhost:8081}") String baseUrl,
                                    @Value("${identity.connect-timeout-ms:1500}") int connectTimeoutMs,
                                    @Value("${identity.read-timeout-ms:2500}") int readTimeoutMs) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeoutMs);
        requestFactory.setReadTimeout(readTimeoutMs);
        this.restClient = RestClient.builder().baseUrl(baseUrl).requestFactory(requestFactory).build();
    }

    public Optional<Instant> accountCreatedAt(UUID userId) {
        Instant cached = cache.get(userId);
        if (cached != null) {
            return Optional.of(cached);
        }
        try {
            JsonNode response = restClient.get().uri("/api/v1/users/{id}/account-created-at", userId)
                    .retrieve().body(JsonNode.class);
            JsonNode value = response == null ? null : response.path("data").path("accountCreatedAt");
            if (value == null || value.isMissingNode() || value.isNull()) {
                return Optional.empty();
            }
            Instant createdAt = Instant.parse(value.asText());
            if (createdAt.isAfter(Instant.now())) {
                return Optional.empty();
            }
            cache.put(userId, createdAt);
            return Optional.of(createdAt);
        } catch (Exception exception) {
            log.warn("Identity account-created-at lookup failed. userId={}", userId, exception);
            return Optional.empty();
        }
    }

    public Instant accountCreatedAtForGrant(UUID userId) {
        return accountCreatedAt(userId).orElseThrow(() -> new AccountAgeUnavailableException(userId));
    }
}
