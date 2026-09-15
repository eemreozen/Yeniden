package com.yeniden.ecocoin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/** Internal request used for non-handover grants such as completed quests. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GrantCoinsRequest {
    private UUID userId;
    private String idempotencyKey;
    private long amount;
    private String reason;
    private String sourceRef;
}
