package com.yeniden.exchange.controller;

import com.yeniden.common.result.ApiResponse;
import com.yeniden.exchange.dto.ConfirmCodeRequest;
import com.yeniden.exchange.dto.CreateRequestDto;
import com.yeniden.exchange.dto.HandoverDto;
import com.yeniden.exchange.dto.ListingRequestDto;
import com.yeniden.exchange.service.ExchangeService;
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
 * Exchange Service REST API Uç Noktaları.
 */
@RestController
@RequestMapping("/api/v1/exchange")
@RequiredArgsConstructor
public class ExchangeController {

    private final ExchangeService exchangeService;

    @GetMapping("/health")
    public ResponseEntity<ApiResponse<String>> healthCheck() {
        return ResponseEntity.ok(ApiResponse.success("Exchange Service çalışıyor!"));
    }

    @PostMapping("/listings/{id}/requests")
    public ResponseEntity<ApiResponse<ListingRequestDto>> createRequest(
            @PathVariable("id") UUID listingId,
            @RequestBody CreateRequestDto requestDto) {

        ListingRequestDto dto = exchangeService.createRequest(listingId, requestDto);
        return ResponseEntity.ok(ApiResponse.success("İlan talebi başarıyla iletildi", dto));
    }

    @GetMapping("/listings/{id}/requests")
    public ResponseEntity<ApiResponse<List<ListingRequestDto>>> getRequestsByListing(
            @PathVariable("id") UUID listingId) {

        List<ListingRequestDto> list = exchangeService.getRequestsByListing(listingId);
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @PostMapping("/requests/{id}/accept")
    public ResponseEntity<ApiResponse<HandoverDto>> acceptRequest(
            @PathVariable("id") UUID requestId,
            @RequestParam(name = "ownerId") UUID ownerId) {

        HandoverDto dto = exchangeService.acceptRequest(requestId, ownerId);
        return ResponseEntity.ok(ApiResponse.success("İlan talebi onaylandı ve teslimat kodu oluşturuldu", dto));
    }

    @GetMapping("/handovers/{id}/code")
    public ResponseEntity<ApiResponse<HandoverDto>> getHandoverCode(
            @PathVariable("id") UUID handoverId,
            @RequestParam(name = "userId") UUID userId) {

        HandoverDto dto = exchangeService.getHandoverCode(handoverId, userId);
        return ResponseEntity.ok(ApiResponse.success(dto));
    }

    @PostMapping("/handovers/{id}/confirm")
    public ResponseEntity<ApiResponse<HandoverDto>> confirmHandoverCode(
            @PathVariable("id") UUID handoverId,
            @RequestBody ConfirmCodeRequest request) {

        HandoverDto dto = exchangeService.confirmHandoverCode(handoverId, request);
        return ResponseEntity.ok(ApiResponse.success("Teslimat kodu doğrulandı ve teslimat tamamlandı!", dto));
    }
}
