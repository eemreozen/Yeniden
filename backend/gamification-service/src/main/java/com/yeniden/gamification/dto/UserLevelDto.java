package com.yeniden.gamification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Kullanıcı seviye ve puan durumunu sunan DTO.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserLevelDto {
    private UUID userId;
    private int level;
    private int totalPointsEarned;
    private int pointsToNextLevel;
}
