package com.yeniden.ecocoin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class CoinGrantResult {
    private final boolean granted;
    private final long amount;
    private final CoinGrantReason reason;
    private final WalletDto wallet;
}
