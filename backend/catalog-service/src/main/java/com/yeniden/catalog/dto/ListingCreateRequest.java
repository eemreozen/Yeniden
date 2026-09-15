package com.yeniden.catalog.dto;

import com.yeniden.catalog.domain.ItemCondition;
import com.yeniden.catalog.domain.QuantityBand;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * İlan oluşturma isteği için kullanılan DTO.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ListingCreateRequest {
    private UUID ownerId;
    private UUID categoryId;
    private String title;
    private String description;
    private QuantityBand quantityBand;
    private ItemCondition condition;
    private double latitude;
    private double longitude;
    private UUID neighborhoodId;
}
