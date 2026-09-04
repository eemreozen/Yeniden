package com.yeniden.catalog.dto;

import com.yeniden.catalog.domain.ItemCondition;
import com.yeniden.catalog.domain.QuantityBand;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * İlan güncelleme isteği için kullanılan DTO. Alanlar opsiyoneldir; yalnızca doldurulanlar uygulanır.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ListingUpdateRequest {
    private String title;
    private String description;
    private QuantityBand quantityBand;
    private ItemCondition condition;
}
