package com.yeniden.exchange.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.yeniden.common.exception.BaseException;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class CatalogRewardContextClient {
    private final RestClient restClient;

    public CatalogRewardContextClient(@Value("${catalog.base-url:http://localhost:8082}") String baseUrl,
                                      @Value("${catalog.connect-timeout-ms:1500}") int connectTimeoutMs,
                                      @Value("${catalog.read-timeout-ms:2500}") int readTimeoutMs) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeoutMs);
        requestFactory.setReadTimeout(readTimeoutMs);
        restClient = RestClient.builder().baseUrl(baseUrl).requestFactory(requestFactory).build();
    }

    public ListingRewardContext get(UUID listingId) {
        try {
            JsonNode response = restClient.get()
                    .uri("/internal/catalog/listings/{id}/reward-context", listingId)
                    .retrieve()
                    .body(JsonNode.class);
            JsonNode data = response == null ? null : response.path("data");
            if (data == null || data.isMissingNode() || data.isNull()) {
                throw unavailable();
            }
            return new ListingRewardContext(
                    UUID.fromString(data.path("listingId").asText()),
                    UUID.fromString(data.path("ownerId").asText()),
                    UUID.fromString(data.path("categoryId").asText()),
                    new BigDecimal(data.path("categoryCoinMultiplier").asText()),
                    data.path("quantityBand").asText(),
                    data.path("listingStatus").asText());
        } catch (BaseException exception) {
            throw exception;
        } catch (Exception exception) {
            throw unavailable();
        }
    }

    private BaseException unavailable() {
        return new BaseException("İlan ödül bilgisi alınamadı.", "CATALOG_REWARD_CONTEXT_UNAVAILABLE", 503);
    }
}
