package com.yeniden.wasteai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Görüntü sınıflandırma sonucu DTO.
 * category.json şablonuyla %100 uyumludur.
 * Model güven skoru %75'in altına düştüğünde requires_moderation=true olur.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassifyImageResponse {

    private String status; // "SUCCESS", "FALLBACK"

    private double confidence; // 0.0 - 1.0 (Token logprob'dan türetilen gerçek olasılık)

    @JsonProperty("requires_moderation")
    private boolean requiresModeration; // %75 altı ise true, doğrudan yayına çıkmaz

    @JsonProperty("moderation_reason")
    private String moderationReason; // Moderatör inceleme sebebi

    @JsonProperty("detected_item")
    private DetectedItemDto detectedItem;

    @JsonProperty("ecocoin_estimate")
    private EcocoinEstimateDto ecocoinEstimate;

    @JsonProperty("recycle_fallback")
    private RecycleFallbackDto recycleFallback;

    // --- Geriye Dönük Uyumluluk (Backward Compatibility) Metotları ---

    public String getSuggestedCategoryCode() {
        return detectedItem != null ? detectedItem.getCategory() : null;
    }

    public String getSuggestedCategoryName() {
        return detectedItem != null ? detectedItem.getName() : null;
    }

    public double getConfidenceScore() {
        return confidence;
    }

    public int getEstimatedPoints() {
        return ecocoinEstimate != null ? ecocoinEstimate.getEstimatedReward() : 0;
    }

    public String getDescription() {
        return detectedItem != null ? detectedItem.getActionReason() : null;
    }
}
