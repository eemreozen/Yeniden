package com.yeniden.catalog.service;

import com.yeniden.catalog.domain.ListingStatus;
import com.yeniden.catalog.domain.QuantityBand;
import com.yeniden.catalog.dto.ListingCreateRequest;
import com.yeniden.catalog.dto.ListingDto;
import com.yeniden.catalog.dto.ListingRewardContextDto;
import com.yeniden.catalog.dto.ListingUpdateRequest;

import java.util.List;
import java.util.UUID;

/**
 * İlan iş mantığı (Business Logic) arayüzü.
 */
public interface ListingService {
    ListingDto createListing(ListingCreateRequest request);
    ListingDto publishListing(UUID id);
    ListingDto getListingById(UUID id);
    ListingRewardContextDto getRewardContext(UUID id);
    List<ListingDto> getAllPublishedListings();

    /**
     * PostGIS ST_DWithin ile gerçek yakınlık araması (05-api.md: radius metre, üst sınır 25000;
     * categoryId alt kategorileri de kapsar; sort "distance" veya "newest"; limit üst sınırı 50).
     */
    List<ListingDto> searchNearby(double lat, double lon, double radiusMeters, UUID categoryId,
                                   QuantityBand quantityBand, String query, String sort, int limit);

    List<ListingDto> getListingsByOwner(UUID ownerId, ListingStatus status);
    ListingDto withdrawListing(UUID id, UUID ownerId);
    ListingDto updateListing(UUID id, UUID ownerId, ListingUpdateRequest request);

    /** exchange-service'in talep kabulünde çağırması beklenen rezervasyon ucu (03-flows.md: markReserved). */
    ListingDto reserveListing(UUID id, UUID requestId);

    /** Rezervasyon süresi dolduğunda/iptal edildiğinde exchange-service'in çağırması beklenir. */
    ListingDto releaseReservation(UUID id);
}
