package com.yeniden.ecocoin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Puan verme isteği DTO.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GrantCoinsRequest {
    private UUID userId;
    private UUID handoverId;
    private String idempotencyKey;
    private int amount; // Hak edilen puan (Örn: 30)
    private String description;
}
