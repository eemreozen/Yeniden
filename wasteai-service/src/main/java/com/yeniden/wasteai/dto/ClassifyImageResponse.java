package com.yeniden.wasteai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Görüntü sınıflandırma sonucu DTO.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassifyImageResponse {
    private String suggestedCategoryCode;
    private String suggestedCategoryName;
    private double confidenceScore; // Örn: 0.95 (%95 doğruluk tahmini)
    private int estimatedPoints;     // Örn: 30 Eco-Coin
    private String description;
}
