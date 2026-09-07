package com.yeniden.catalog.service;

import com.yeniden.common.exception.BaseException;
import com.yeniden.catalog.domain.Listing;
import com.yeniden.catalog.domain.ListingStatus;
import com.yeniden.catalog.dto.ListingCreateRequest;
import com.yeniden.catalog.dto.ListingDto;
import com.yeniden.catalog.dto.ListingRewardContextDto;
import com.yeniden.catalog.domain.ItemCategory;
import com.yeniden.catalog.repository.ItemCategoryRepository;
import com.yeniden.catalog.repository.ListingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * İlan iş mantığı gerçeklemesi (Business Logic Implementation).
 */
@Service
@RequiredArgsConstructor
public class ListingServiceImpl implements ListingService {

    private final ListingRepository listingRepository;
    private final ItemCategoryRepository itemCategoryRepository;
    private final Random random = new Random();

    @Override
    @Transactional
    public ListingDto createListing(ListingCreateRequest request) {
        // ADR-007: Konum gizliliği için gerçek noktadan ~250m rastgele kaydırma (jitter) yapılır ve sabit saklanır
        double approxLat = request.getLatitude() + (random.nextDouble() - 0.5) * 0.004;
        double approxLon = request.getLongitude() + (random.nextDouble() - 0.5) * 0.004;

        Listing listing = Listing.builder()
                .ownerId(request.getOwnerId())
                .categoryId(request.getCategoryId())
                .title(request.getTitle())
                .description(request.getDescription())
                .quantityBand(request.getQuantityBand())
                .condition(request.getCondition())
                .status(ListingStatus.DRAFT)
                .approxLatitude(approxLat)
                .approxLongitude(approxLon)
                .neighborhoodId(request.getNeighborhoodId())
                .build();

        Listing saved = listingRepository.save(listing);
        return mapToDto(saved);
    }

    @Override
    @Transactional
    public ListingDto publishListing(UUID id) {
        Listing listing = listingRepository.findById(id)
                .orElseThrow(() -> new BaseException("İlan bulunamadı!", "LISTING_NOT_FOUND", 404));

        listing.setStatus(ListingStatus.PUBLISHED);
        listing.setPublishedAt(LocalDateTime.now());
        listing.setExpiresAt(LocalDateTime.now().plusDays(21)); // 21 gün geçerli

        Listing updated = listingRepository.save(listing);
        return mapToDto(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public ListingDto getListingById(UUID id) {
        Listing listing = listingRepository.findById(id)
                .orElseThrow(() -> new BaseException("İlan bulunamadı!", "LISTING_NOT_FOUND", 404));

        return mapToDto(listing);
    }

    @Override
    @Transactional(readOnly = true)
    public ListingRewardContextDto getRewardContext(UUID id) {
        Listing listing = listingRepository.findById(id)
                .orElseThrow(() -> new BaseException("İlan bulunamadı!", "LISTING_NOT_FOUND", 404));
        ItemCategory category = itemCategoryRepository.findById(listing.getCategoryId())
                .orElseThrow(() -> new BaseException("İlan kategorisi bulunamadı!", "CATEGORY_NOT_FOUND", 409));
        return new ListingRewardContextDto(
                listing.getId(),
                listing.getOwnerId(),
                listing.getCategoryId(),
                java.math.BigDecimal.valueOf(category.getCoinMultiplier()),
                listing.getQuantityBand().name(),
                listing.getStatus().name());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ListingDto> getAllPublishedListings() {
        return listingRepository.findByStatus(ListingStatus.PUBLISHED)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ListingDto> searchNearby(double lat, double lon, double radiusKm) {
        // Yaklaşık derece sapma hesabı (1 derece ~ 111 km)
        double latDelta = radiusKm / 111.0;
        double lonDelta = radiusKm / (111.0 * Math.cos(Math.toRadians(lat)));

        return listingRepository.findNearbyListings(
                        ListingStatus.PUBLISHED,
                        lat - latDelta, lat + latDelta,
                        lon - lonDelta, lon + lonDelta
                ).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    private ListingDto mapToDto(Listing listing) {
        return ListingDto.builder()
                .id(listing.getId())
                .ownerId(listing.getOwnerId())
                .categoryId(listing.getCategoryId())
                .title(listing.getTitle())
                .description(listing.getDescription())
                .quantityBand(listing.getQuantityBand())
                .condition(listing.getCondition())
                .status(listing.getStatus())
                .approxLatitude(listing.getApproxLatitude())
                .approxLongitude(listing.getApproxLongitude())
                .neighborhoodId(listing.getNeighborhoodId())
                .publishedAt(listing.getPublishedAt())
                .expiresAt(listing.getExpiresAt())
                .createdAt(listing.getCreatedAt())
                .build();
    }
}
