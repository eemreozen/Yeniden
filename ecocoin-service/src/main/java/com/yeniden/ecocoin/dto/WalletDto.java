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
    private int balance;
    private int dailyEarnedToday;
    private int monthlyEarnedThisMonth;
    private int dailyCapRemaining;  // 100 - dailyEarnedToday
    private int monthlyCapRemaining; // 1200 - monthlyEarnedThisMonth
}
