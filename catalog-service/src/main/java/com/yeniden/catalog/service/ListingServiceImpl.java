package com.yeniden.catalog.service;

import com.yeniden.catalog.config.RabbitMQConfig;
import com.yeniden.common.event.ListingPublishedEvent;
import com.yeniden.common.exception.BaseException;
import com.yeniden.catalog.domain.Listing;
import com.yeniden.catalog.domain.ListingStatus;
import com.yeniden.catalog.domain.QuantityBand;
import com.yeniden.catalog.dto.ListingCreateRequest;
import com.yeniden.catalog.dto.ListingDto;
import com.yeniden.catalog.dto.ListingRewardContextDto;
import com.yeniden.catalog.domain.ItemCategory;
import com.yeniden.catalog.repository.ItemCategoryRepository;
import com.yeniden.catalog.dto.ListingUpdateRequest;
import com.yeniden.catalog.repository.ListingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
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
@Slf4j
@Service
@RequiredArgsConstructor
public class ListingServiceImpl implements ListingService {

    private static final double EARTH_RADIUS_METERS = 6_371_000.0;

    private final ListingRepository listingRepository;
    private final ItemCategoryRepository itemCategoryRepository;
    private final CategoryService categoryService;
    private final RabbitTemplate rabbitTemplate;
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
        publishListingPublishedEvent(updated);
        return mapToDto(updated);
    }

    private void publishListingPublishedEvent(Listing listing) {
        try {
            ListingPublishedEvent event = ListingPublishedEvent.builder()
                    .listingId(listing.getId())
                    .ownerId(listing.getOwnerId())
                    .categoryId(listing.getCategoryId())
                    .publishedAt(listing.getPublishedAt())
                    .build();

            rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, RabbitMQConfig.LISTING_PUBLISHED_ROUTING_KEY, event);
        } catch (Exception e) {
            // RabbitMQ gönderim hatası ilan yayınlama akışını bozmamalıdır
            log.error("ListingPublished event gönderme hatası: {}", e.getMessage());
        }
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
    public List<ListingDto> searchNearby(double lat, double lon, double radiusMeters, UUID categoryId,
                                          QuantityBand quantityBand, String query, String sort, int limit) {
        List<UUID> categoryIds = categoryId != null ? categoryService.getSubtreeIds(categoryId) : null;

        return listingRepository.searchNearby(lat, lon, radiusMeters, categoryIds, quantityBand, query, sort, limit)
                .stream()
                .map(listing -> mapToDto(listing, distanceMetersRoundedTo100(lat, lon,
                        listing.getApproxLatitude(), listing.getApproxLongitude())))
                .collect(Collectors.toList());
    }

    /** Haversine (great-circle) mesafe hesabı; 05-api.md gereği sonuç 100 m'ye yuvarlanır. */
    private static int distanceMetersRoundedTo100(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double meters = EARTH_RADIUS_METERS * c;
        return (int) Math.round(meters / 100.0) * 100;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ListingDto> getListingsByOwner(UUID ownerId, ListingStatus status) {
        List<Listing> listings = status != null
                ? listingRepository.findByOwnerIdAndStatus(ownerId, status)
                : listingRepository.findByOwnerId(ownerId);

        return listings.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ListingDto withdrawListing(UUID id, UUID ownerId) {
        Listing listing = listingRepository.findById(id)
                .orElseThrow(() -> new BaseException("İlan bulunamadı!", "LISTING_NOT_FOUND", 404));

        if (!listing.getOwnerId().equals(ownerId)) {
            throw new BaseException("Bu ilanı geri çekme yetkiniz yok!", "FORBIDDEN", 403);
        }
        if (listing.getStatus() != ListingStatus.PUBLISHED) {
            throw new BaseException("Yalnızca yayındaki ilanlar geri çekilebilir!", "LISTING_NOT_PUBLISHED", 409);
        }

        listing.setStatus(ListingStatus.WITHDRAWN);
        Listing updated = listingRepository.save(listing);
        return mapToDto(updated);
    }

    @Override
    @Transactional
    public ListingDto updateListing(UUID id, UUID ownerId, ListingUpdateRequest request) {
        Listing listing = listingRepository.findById(id)
                .orElseThrow(() -> new BaseException("İlan bulunamadı!", "LISTING_NOT_FOUND", 404));

        if (!listing.getOwnerId().equals(ownerId)) {
            throw new BaseException("Bu ilanı güncelleme yetkiniz yok!", "FORBIDDEN", 403);
        }
        if (listing.getStatus() != ListingStatus.DRAFT && listing.getStatus() != ListingStatus.PUBLISHED) {
            throw new BaseException("İlan yalnızca taslak veya yayın durumundayken güncellenebilir!", "LISTING_NOT_EDITABLE", 409);
        }

        if (request.getTitle() != null) listing.setTitle(request.getTitle());
        if (request.getDescription() != null) listing.setDescription(request.getDescription());
        if (request.getQuantityBand() != null) listing.setQuantityBand(request.getQuantityBand());
        if (request.getCondition() != null) listing.setCondition(request.getCondition());

        Listing updated = listingRepository.save(listing);
        return mapToDto(updated);
    }

    @Override
    @Transactional
    public ListingDto reserveListing(UUID id, UUID requestId) {
        int updated = listingRepository.reserveIfCurrentStatus(id, requestId, ListingStatus.RESERVED, ListingStatus.PUBLISHED);
        if (updated == 0) {
            throw new BaseException("İlan artık müsait değil!", "LISTING_NOT_AVAILABLE", 409);
        }
        return getListingById(id);
    }

    @Override
    @Transactional
    public ListingDto releaseReservation(UUID id) {
        Listing listing = listingRepository.findById(id)
                .orElseThrow(() -> new BaseException("İlan bulunamadı!", "LISTING_NOT_FOUND", 404));

        if (listing.getStatus() == ListingStatus.RESERVED) {
            listing.setStatus(ListingStatus.PUBLISHED);
            listing.setReservedRequestId(null);
            listing = listingRepository.save(listing);
        }
        return mapToDto(listing);
    }

    private ListingDto mapToDto(Listing listing) {
        return mapToDto(listing, null);
    }

    private ListingDto mapToDto(Listing listing, Integer distanceMeters) {
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
                .distanceMeters(distanceMeters)
                .build();
    }
}
