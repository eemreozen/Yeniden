package com.yeniden.wasteai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Model çıktısına göre hesaplanan tahmini Eco-Coin ödül bilgisi.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EcocoinEstimateDto {

    @JsonProperty("base_points")
    private int basePoints;

    @JsonProperty("category_multiplier")
    private double categoryMultiplier;

    @JsonProperty("estimated_reward")
    private int estimatedReward;
}
