package com.yeniden.wasteai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Model tarafından tespit edilen eşya ve durum analizi.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DetectedItemDto {

    private String name;

    private String category;

    @JsonProperty("condition_grade")
    private String conditionGrade; // Örn: "NEW", "GOOD", "FAIR", "POOR_RECYCLE"

    @JsonProperty("is_damaged")
    private boolean isDamaged;

    @JsonProperty("suggested_action")
    private ActionType suggestedAction; // REUSE / RECYCLE

    @JsonProperty("action_reason")
    private String actionReason;
}
