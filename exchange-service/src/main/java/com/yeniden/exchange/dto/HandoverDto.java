package com.yeniden.exchange.dto;

import com.yeniden.exchange.domain.HandoverStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Teslimat bilgilerini sunan DTO.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HandoverDto {
    private UUID id;
    private UUID requestId;
    private UUID listingId;
    private UUID providerId;
    private UUID receiverId;
    private String confirmationCode; // Yalnızca yetkili alıcı istek attığında doldurulur
    private HandoverStatus status;
    private int failedAttempts;
    private LocalDateTime expiresAt;
    private LocalDateTime confirmedAt;
}
