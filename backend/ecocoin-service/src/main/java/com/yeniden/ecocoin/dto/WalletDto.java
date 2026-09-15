package com.yeniden.ecocoin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Kullanıcı cüzdan ve tavan durumunu sunan DTO.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletDto {
    private UUID userId;
    private long balance;
    private long totalEarned;
    private long dailyEarnedToday;
    private long monthlyEarnedThisMonth;
    private long dailyCapRemaining;
    private long monthlyCapRemaining;
}
