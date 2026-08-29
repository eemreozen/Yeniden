package com.yeniden.gamification.service;

import com.yeniden.gamification.dto.BadgeDto;
import com.yeniden.gamification.dto.UserBadgeDto;
import com.yeniden.gamification.dto.UserLevelDto;

import java.util.List;
import java.util.UUID;

/**
 * Gamification Service İş Mantığı Arayüzü.
 */
public interface GamificationService {
    List<BadgeDto> getAllBadges();
    List<UserBadgeDto> getUserBadges(UUID userId);
    UserLevelDto getUserLevel(UUID userId);
    UserBadgeDto awardBadge(UUID userId, String badgeCode);
    UserLevelDto addPoints(UUID userId, int points);
}
