package com.yeniden.catalog.controller;

import com.yeniden.common.result.ApiResponse;
import com.yeniden.catalog.dto.ListingCreateRequest;
import com.yeniden.catalog.dto.ListingDto;
import com.yeniden.catalog.service.ListingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
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
            @RequestParam(name = "radius", required = false, defaultValue = "5.0") Double radiusKm) {

        if (lat != null && lon != null) {
            List<ListingDto> nearby = listingService.searchNearby(lat, lon, radiusKm);
            return ResponseEntity.ok(ApiResponse.success(nearby));
        }

        List<ListingDto> published = listingService.getAllPublishedListings();
        return ResponseEntity.ok(ApiResponse.success(published));
    }
}
