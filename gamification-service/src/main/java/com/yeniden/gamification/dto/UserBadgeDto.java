package com.yeniden.gamification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Kullanıcıya tanımlı rozet DTO.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserBadgeDto {
    private UUID id;
    private UUID userId;
    private String badgeCode;
    private String badgeName;
    private String description;
    private String iconKey;
    private LocalDateTime earnedAt;
}
