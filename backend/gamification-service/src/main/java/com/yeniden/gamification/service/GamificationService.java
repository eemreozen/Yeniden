package com.yeniden.gamification.service;

import com.yeniden.common.event.CoinsGrantedEvent;
import com.yeniden.common.event.HandoverConfirmedEvent;
import com.yeniden.common.event.ListingPublishedEvent;
import com.yeniden.gamification.dto.BadgeDto;
import com.yeniden.gamification.dto.UserBadgeDto;
import com.yeniden.gamification.dto.UserLevelDto;
import com.yeniden.gamification.dto.QuestDto;
import com.yeniden.gamification.dto.LeaderboardDto;
import java.util.List;
import java.util.UUID;

public interface GamificationService {
    void handleCoinsGranted(CoinsGrantedEvent event);
    void handleHandoverConfirmed(HandoverConfirmedEvent event);
    void handleListingPublished(ListingPublishedEvent event);
    List<BadgeDto> getAllBadges();
    List<UserBadgeDto> getUserBadges(UUID userId);
    UserLevelDto getUserLevel(UUID userId);
    List<QuestDto> getQuests(String period);
    List<LeaderboardDto> getLeaderboard(String period);
}
