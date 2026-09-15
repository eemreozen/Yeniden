package com.yeniden.catalog.repository;

import com.yeniden.catalog.domain.Listing;
import com.yeniden.catalog.domain.QuantityBand;

import java.util.List;
import java.util.UUID;

/**
 * PostGIS ST_DWithin tabanlı yakınlık araması için özel (custom) repository sözleşmesi.
 * Spring Data'nın "fragment interface + Impl" desenine göre {@link ListingSearchRepositoryImpl}
 * ile gerçeklenir ve {@link ListingRepository} içine karışır (mix-in).
 */
public interface ListingSearchRepository {
    List<Listing> searchNearby(double lat, double lon, double radiusMeters,
                                List<UUID> categoryIds, QuantityBand quantityBand,
                                String searchText, String sort, int limit);
}
