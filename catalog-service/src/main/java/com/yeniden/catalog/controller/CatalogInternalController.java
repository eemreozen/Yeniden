package com.yeniden.catalog.controller;

import com.yeniden.catalog.dto.ListingRewardContextDto;
import com.yeniden.catalog.service.ListingService;
import com.yeniden.common.result.ApiResponse;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/catalog")
@RequiredArgsConstructor
public class CatalogInternalController {
    private final ListingService listingService;

    @GetMapping("/listings/{id}/reward-context")
    public ResponseEntity<ApiResponse<ListingRewardContextDto>> getRewardContext(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(listingService.getRewardContext(id)));
    }
}
