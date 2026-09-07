package com.yeniden.catalog.dto;

import java.math.BigDecimal;
import java.util.UUID;

/** Immutable reward inputs captured by exchange when a handover starts. */
public record ListingRewardContextDto(
        UUID listingId,
        UUID ownerId,
        UUID categoryId,
        BigDecimal categoryCoinMultiplier,
        String quantityBand,
        String listingStatus) {
}
