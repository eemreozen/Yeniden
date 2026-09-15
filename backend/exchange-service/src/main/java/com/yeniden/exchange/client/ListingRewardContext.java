package com.yeniden.exchange.client;

import java.math.BigDecimal;
import java.util.UUID;

public record ListingRewardContext(
        UUID listingId,
        UUID ownerId,
        UUID categoryId,
        BigDecimal categoryCoinMultiplier,
        String quantityBand,
        String listingStatus) {
}
