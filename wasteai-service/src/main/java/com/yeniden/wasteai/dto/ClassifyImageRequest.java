package com.yeniden.wasteai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Görüntü sınıflandırma isteği DTO.
 * Hem S3 URL'si hem de doğrudan Base64 kodlanmış görüntü desteklenir.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassifyImageRequest {

    private String imageUrl;

    private String imageBase64;

    private Double latitude;

    private Double longitude;
}
