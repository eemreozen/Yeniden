package com.yeniden.ecocoin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Çift kayıtlı defter hareket detayını sunan DTO.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoinEntryDto {
    private UUID id;
    private UUID transactionId;
    private String account;
    private int amount;
    private LocalDateTime createdAt;
}
