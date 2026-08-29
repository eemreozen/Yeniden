package com.yeniden.wasteai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Görüntü sınıflandırma isteği DTO.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassifyImageRequest {
    private String imageUrl;
    private Double latitude;
    private Double longitude;
}
