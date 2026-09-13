package com.yeniden.wasteai.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yeniden.wasteai.dto.ActionType;
import com.yeniden.wasteai.dto.ClassifyImageRequest;
import com.yeniden.wasteai.dto.ClassifyImageResponse;
import com.yeniden.wasteai.dto.DetectedItemDto;
import com.yeniden.wasteai.dto.EcocoinEstimateDto;
import com.yeniden.wasteai.dto.RecycleFallbackDto;
import com.yeniden.wasteai.service.ConfidenceCalculator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * SOLID - SRP & LSP:
 * Kullanıcının yerel donanımında (NVIDIA RTX 4050 GPU) koşan
 * yerel görsel modellerle (Ollama / Qwen2-VL / LLaVA / ONNX) haberleşen sağlayıcı.
 */
@Slf4j
@Component
public class LocalVisionModelProvider implements AiModelProvider {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${wasteai.local.enabled:true}")
    private boolean enabled;

    @Value("${wasteai.local.base-url:http://localhost:11434}")
    private String baseUrl;

    @Value("${wasteai.local.model:qwen2-vl}")
    private String modelName;

    public LocalVisionModelProvider() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    public LocalVisionModelProvider(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public ClassifyImageResponse analyze(ClassifyImageRequest request) {
        log.info("LocalVisionModelProvider çalışıyor. Model: {}, URL: {}", modelName, baseUrl);

        try {
            String prompt = buildPrompt();
            Map<String, Object> payload = new HashMap<>();
            payload.put("model", modelName);
            payload.put("format", "json");
            payload.put("stream", false);

            Map<String, Object> message = new HashMap<>();
            message.put("role", "user");
            message.put("content", prompt);

            // Görüntü ekleme (Base64 veya URL)
            if (request.getImageBase64() != null && !request.getImageBase64().isBlank()) {
                message.put("images", List.of(request.getImageBase64()));
            }

            payload.put("messages", Collections.singletonList(message));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(baseUrl + "/api/chat", entity, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return parseLocalResponse(response.getBody());
            }
        } catch (RestClientException e) {
            log.warn("Yerel model servisine erişilemedi (Ollama çalışmıyor olabilir): {}", e.getMessage());
            throw new RuntimeException("Local model runtime unavailable", e);
        } catch (Exception e) {
            log.error("Yerel model analizinde hata oluştu: {}", e.getMessage(), e);
            throw new RuntimeException("Local model inference error", e);
        }

        throw new RuntimeException("Local model empty response");
    }

    private ClassifyImageResponse parseLocalResponse(String responseBody) throws Exception {
        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode messageContent = root.path("message").path("content");

        String contentStr = messageContent.asText();
        JsonNode data = objectMapper.readTree(contentStr);

        String itemName = data.path("name").asText("Oluklu Mukavva Koli Kutusu");
        String category = data.path("category").asText("CARDBOARD_BOX");
        String conditionGrade = data.path("condition_grade").asText("GOOD");
        boolean isDamaged = data.path("is_damaged").asBoolean(false);
        String actionStr = data.path("suggested_action").asText("REUSE");
        String reason = data.path("action_reason").asText("Yerel yapay zeka analizi tamamlandı.");

        ActionType action = "RECYCLE".equalsIgnoreCase(actionStr) ? ActionType.RECYCLE : ActionType.REUSE;
        double confidence = data.has("confidence") ? data.path("confidence").asDouble(0.92) : 0.92;

        return ClassifyImageResponse.builder()
                .status("SUCCESS")
                .confidence(confidence)
                .detectedItem(DetectedItemDto.builder()
                        .name(itemName)
                        .category(category)
                        .conditionGrade(conditionGrade)
                        .isDamaged(isDamaged)
                        .suggestedAction(action)
                        .actionReason(reason)
                        .build())
                .ecocoinEstimate(EcocoinEstimateDto.builder()
                        .basePoints(10)
                        .categoryMultiplier(action == ActionType.REUSE ? 1.0 : 0.5)
                        .estimatedReward(action == ActionType.REUSE ? 15 : 5)
                        .build())
                .recycleFallback(RecycleFallbackDto.builder()
                        .requiresRecyclePoint(action == ActionType.RECYCLE)
                        .nearestHubCategory(action == ActionType.RECYCLE ? "Belediye Atık Toplama Noktası" : null)
                        .build())
                .build();
    }

    private String buildPrompt() {
        return """
            Bu fotoğrafı Sıfır Atık ve Döngüsel Ekonomi platformu için analiz et.
            Yanıtı KESİNLİKLE sadece aşağıdaki JSON şemasında döndür:
            {
              "name": "Eşya Tanımı (örn: Oluklu Mukavva Koli Kutusu)",
              "category": "CARDBOARD_BOX veya GLASS_JAR veya WOODEN_PALLET veya RECYCLE_WASTE",
              "condition_grade": "GOOD veya FAIR veya POOR_RECYCLE",
              "is_damaged": false,
              "suggested_action": "REUSE veya RECYCLE",
              "action_reason": "Kullanıcı için 1 cümlelik gerekçe",
              "confidence": 0.94
            }
            """;
    }

    @Override
    public boolean isAvailable() {
        return enabled;
    }

    @Override
    public String getProviderName() {
        return "LOCAL_VISION_MODEL";
    }

    @Override
    public int getOrder() {
        return 20; // Yerel GPU önceliği
    }
}
