package com.yeniden.catalog.controller;

import com.yeniden.common.result.ApiResponse;
import com.yeniden.catalog.domain.ListingStatus;
import com.yeniden.catalog.domain.QuantityBand;
import com.yeniden.catalog.dto.ListingCreateRequest;
import com.yeniden.catalog.dto.ListingDto;
import com.yeniden.catalog.dto.ListingUpdateRequest;
import com.yeniden.catalog.service.ListingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Catalog Service REST API Uç Noktaları.
 */
@RestController
@RequestMapping("/api/v1/catalog")
@RequiredArgsConstructor
public class ListingController {

    private final ListingService listingService;

    @GetMapping("/health")
    public ResponseEntity<ApiResponse<String>> healthCheck() {
        return ResponseEntity.ok(ApiResponse.success("Catalog Service çalışıyor!"));
    }

    @PostMapping("/listings")
    public ResponseEntity<ApiResponse<ListingDto>> createListing(@RequestBody ListingCreateRequest request) {
        ListingDto dto = listingService.createListing(request);
        return ResponseEntity.ok(ApiResponse.success("İlan taslağı oluşturuldu", dto));
    }

    @PostMapping("/listings/{id}/publish")
    public ResponseEntity<ApiResponse<ListingDto>> publishListing(@PathVariable("id") UUID id) {
        ListingDto dto = listingService.publishListing(id);
        return ResponseEntity.ok(ApiResponse.success("İlan başarıyla yayınlandı", dto));
    }

    @GetMapping("/listings/{id}")
    public ResponseEntity<ApiResponse<ListingDto>> getListingById(@PathVariable("id") UUID id) {
        ListingDto dto = listingService.getListingById(id);
        return ResponseEntity.ok(ApiResponse.success(dto));
    }

    @GetMapping("/listings")
    public ResponseEntity<ApiResponse<List<ListingDto>>> getListings(
            @RequestParam(name = "lat", required = false) Double lat,
            @RequestParam(name = "lon", required = false) Double lon,
            @RequestParam(name = "radius", required = false, defaultValue = "3000") Double radiusMeters,
            @RequestParam(name = "categoryId", required = false) UUID categoryId,
            @RequestParam(name = "quantityBand", required = false) QuantityBand quantityBand,
            @RequestParam(name = "q", required = false) String query,
            @RequestParam(name = "sort", required = false, defaultValue = "distance") String sort,
            @RequestParam(name = "limit", required = false, defaultValue = "20") Integer limit) {

        if (lat != null && lon != null) {
            double cappedRadius = Math.min(radiusMeters, 25_000);
            int cappedLimit = Math.min(Math.max(limit, 1), 50);
            List<ListingDto> nearby = listingService.searchNearby(
                    lat, lon, cappedRadius, categoryId, quantityBand, query, sort, cappedLimit);
            return ResponseEntity.ok(ApiResponse.success(nearby));
        }

        List<ListingDto> published = listingService.getAllPublishedListings();
        return ResponseEntity.ok(ApiResponse.success(published));
    }

    @GetMapping("/listings/mine")
    public ResponseEntity<ApiResponse<List<ListingDto>>> getMyListings(
            @RequestParam(name = "ownerId") UUID ownerId,
            @RequestParam(name = "status", required = false) ListingStatus status) {

        List<ListingDto> listings = listingService.getListingsByOwner(ownerId, status);
        return ResponseEntity.ok(ApiResponse.success(listings));
    }

    @PatchMapping("/listings/{id}")
    public ResponseEntity<ApiResponse<ListingDto>> updateListing(
            @PathVariable("id") UUID id,
            @RequestParam(name = "ownerId") UUID ownerId,
            @RequestBody ListingUpdateRequest request) {

        ListingDto dto = listingService.updateListing(id, ownerId, request);
        return ResponseEntity.ok(ApiResponse.success("İlan güncellendi", dto));
    }

    @PostMapping("/listings/{id}/withdraw")
    public ResponseEntity<ApiResponse<ListingDto>> withdrawListing(
            @PathVariable("id") UUID id,
            @RequestParam(name = "ownerId") UUID ownerId) {

        ListingDto dto = listingService.withdrawListing(id, ownerId);
        return ResponseEntity.ok(ApiResponse.success("İlan geri çekildi", dto));
    }

    /**
     * exchange-service'in talep kabulünde çağırması beklenen iç uç (03-flows.md: markReserved).
     * İlan artık PUBLISHED değilse 409 LISTING_NOT_AVAILABLE döner (yarış koşulu koruması).
     */
    @PostMapping("/listings/{id}/reserve")
    public ResponseEntity<ApiResponse<ListingDto>> reserveListing(
            @PathVariable("id") UUID id,
            @RequestParam(name = "requestId") UUID requestId) {

        ListingDto dto = listingService.reserveListing(id, requestId);
        return ResponseEntity.ok(ApiResponse.success("İlan rezerve edildi", dto));
    }

    /**
     * exchange-service'in rezervasyon süresi dolduğunda/iptalinde çağırması beklenen iç uç.
     */
    @PostMapping("/listings/{id}/release")
    public ResponseEntity<ApiResponse<ListingDto>> releaseReservation(@PathVariable("id") UUID id) {
        ListingDto dto = listingService.releaseReservation(id);
        return ResponseEntity.ok(ApiResponse.success("İlan rezervasyonu serbest bırakıldı", dto));
    }
}
