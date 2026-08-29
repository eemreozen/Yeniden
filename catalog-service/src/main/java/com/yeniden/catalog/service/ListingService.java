package com.yeniden.catalog.service;

import com.yeniden.catalog.dto.ListingCreateRequest;
import com.yeniden.catalog.dto.ListingDto;

import java.util.List;
import java.util.UUID;

/**
 * İlan iş mantığı (Business Logic) arayüzü.
 */
public interface ListingService {
    ListingDto createListing(ListingCreateRequest request);
    ListingDto publishListing(UUID id);
    ListingDto getListingById(UUID id);
    List<ListingDto> getAllPublishedListings();
    List<ListingDto> searchNearby(double lat, double lon, double radiusKm);
}
