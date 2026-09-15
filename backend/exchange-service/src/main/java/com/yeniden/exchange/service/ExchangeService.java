package com.yeniden.exchange.service;

import com.yeniden.exchange.dto.ConfirmCodeRequest;
import com.yeniden.exchange.dto.CreateRequestDto;
import com.yeniden.exchange.dto.HandoverDto;
import com.yeniden.exchange.dto.ListingRequestDto;

import java.util.List;
import java.util.UUID;

/**
 * Exchange Service İş Mantığı Arayüzü (ADR-005).
 */
public interface ExchangeService {
    ListingRequestDto createRequest(UUID listingId, CreateRequestDto requestDto);
    HandoverDto acceptRequest(UUID requestId, UUID ownerId);
    HandoverDto getHandoverCode(UUID handoverId, UUID userId);
    HandoverDto confirmHandoverCode(UUID handoverId, ConfirmCodeRequest request);
    List<ListingRequestDto> getRequestsByListing(UUID listingId);
}
