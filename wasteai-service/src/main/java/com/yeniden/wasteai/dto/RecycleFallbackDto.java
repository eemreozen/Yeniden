package com.yeniden.wasteai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Geri dönüşüm gerektiğinde toplama noktası yönlendirme bilgisi.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecycleFallbackDto {

    @JsonProperty("requires_recycle_point")
    private boolean requiresRecyclePoint;

    @JsonProperty("nearest_hub_category")
    private String nearestHubCategory;
}
