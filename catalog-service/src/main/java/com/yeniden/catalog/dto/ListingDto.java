package com.yeniden.catalog.dto;

import com.yeniden.catalog.domain.ItemCondition;
import com.yeniden.catalog.domain.ListingStatus;
import com.yeniden.catalog.domain.QuantityBand;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * İlan bilgilerinin istemciye sunulması için kullanılan DTO.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ListingDto {
    private UUID id;
    private UUID ownerId;
    private UUID categoryId;
    private String title;
    private String description;
    private QuantityBand quantityBand;
    private ItemCondition condition;
    private ListingStatus status;
    private double approxLatitude;
    private double approxLongitude;
    private UUID neighborhoodId;
    private LocalDateTime publishedAt;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
    /** Yalnızca yakınlık aramasında doldurulur; 100 m'ye yuvarlanır (05-api.md). */
    private Integer distanceMeters;
}
