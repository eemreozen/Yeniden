package com.yeniden.exchange.dto;

import com.yeniden.exchange.domain.RequestStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * İlan talebi ayrıntılarını istemciye sunan DTO.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ListingRequestDto {
    private UUID id;
    private UUID listingId;
    private UUID requesterId;
    private UUID ownerId;
    private String message;
    private RequestStatus status;
    private LocalDateTime createdAt;
}
