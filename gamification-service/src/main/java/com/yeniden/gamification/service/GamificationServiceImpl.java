package com.yeniden.gamification.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yeniden.common.event.CoinsGrantedEvent;
import com.yeniden.common.event.HandoverConfirmedEvent;
import com.yeniden.common.event.QuestCompletedEvent;
import com.yeniden.gamification.domain.*;
import com.yeniden.gamification.dto.BadgeDto;
import com.yeniden.gamification.dto.UserBadgeDto;
import com.yeniden.gamification.dto.UserLevelDto;
import com.yeniden.gamification.dto.QuestDto;
import com.yeniden.gamification.dto.LeaderboardDto;
import com.yeniden.gamification.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.Instant;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Service @RequiredArgsConstructor
public class GamificationServiceImpl implements GamificationService {
    private final BadgeRepository badgeRepository;
    private final UserBadgeRepository userBadgeRepository;
    private final UserLevelRepository userLevelRepository;
    private final UserGamificationProfileRepository profileRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final UserCategoryRepository userCategoryRepository;
    private final QuestRepository questRepository;
    private final QuestProgressRepository questProgressRepository;
    private final QuestProgressCategoryRepository questProgressCategoryRepository;
    private final LeaderboardSnapshotRepository leaderboardSnapshotRepository;
    private final ObjectMapper objectMapper;
    private final QuestRewardOutboxEventRepository questRewardOutboxEventRepository;

    @Override @Transactional
    public void handleCoinsGranted(CoinsGrantedEvent event) {
        if (!start("coins-granted", event.getEventId())) return;
        UserGamificationProfile profile = profile(event.getUserId());
        profile.setTotalCoinsEarned(profile.getTotalCoinsEarned() + event.getAmount());
        profile.setLevel((int) (profile.getTotalCoinsEarned() / 100) + 1);
        profileRepository.save(profile);
        syncLegacyLevel(profile);
        evaluateBadges(profile);
        if (!"QUEST_COMPLETED".equals(event.getReason())) {
            updateQuestProgress(event.getUserId(), "TOTAL_COINS_EARNED", event.getAmount(), null);
        }
    }

    @Override @Transactional
    public void handleHandoverConfirmed(HandoverConfirmedEvent event) {
        if (event.isReviewRequired() || "PENDING_REVIEW".equals(event.getHandoverStatus()) || !start("handover-confirmed", event.getHandoverId())) return;
        updateHandoverUser(event.getProviderId(), true, event.getCategoryId());
        updateHandoverUser(event.getReceiverId(), false, event.getCategoryId());
    }

    private boolean start(String listener, UUID eventId) {
        if (eventId == null || processedEventRepository.existsById(new ProcessedEvent.ProcessedEventId(listener, eventId))) return false;
        processedEventRepository.save(new ProcessedEvent(listener, eventId));
        return true;
    }

    private void updateHandoverUser(UUID userId, boolean giver, UUID categoryId) {
        if (userId == null) return;
        UserGamificationProfile profile = profile(userId);
        if (giver) profile.setConfirmedGiveCount(profile.getConfirmedGiveCount() + 1); else profile.setConfirmedTakeCount(profile.getConfirmedTakeCount() + 1);
        UserCategory.UserCategoryId categoryKey = new UserCategory.UserCategoryId(userId, categoryId);
        if (categoryId != null && !userCategoryRepository.existsById(categoryKey)) {
            userCategoryRepository.save(new UserCategory(userId, categoryId));
            profile.setDistinctCategoryCount(profile.getDistinctCategoryCount() + 1);
        }
        profileRepository.save(profile);
        evaluateBadges(profile);
        updateQuestProgress(userId, giver ? "CONFIRMED_GIVE_COUNT" : "CONFIRMED_TAKE_COUNT", 1, categoryId);
        if (categoryId != null) updateQuestProgress(userId, "DISTINCT_CATEGORY_COUNT", 0, categoryId);
    }

    private UserGamificationProfile profile(UUID userId) { return profileRepository.findById(userId).orElseGet(() -> UserGamificationProfile.builder().userId(userId).build()); }
    private void syncLegacyLevel(UserGamificationProfile profile) { userLevelRepository.save(UserLevel.builder().userId(profile.getUserId()).level(profile.getLevel()).totalPointsEarned(Math.toIntExact(profile.getTotalCoinsEarned())).build()); }

    private void evaluateBadges(UserGamificationProfile profile) {
        for (Badge badge : badgeRepository.findByActiveTrue()) if (matches(badge.getRule(), profile) && userBadgeRepository.findByUserIdAndBadgeId(profile.getUserId(), badge.getId()).isEmpty())
            userBadgeRepository.save(UserBadge.builder().userId(profile.getUserId()).badgeId(badge.getId()).badgeCode(badge.getCode()).build());
    }

    private void updateQuestProgress(UUID userId, String metric, long delta, UUID categoryId) {
        for (Quest quest : questRepository.findByPeriod(YearMonth.now().toString())) {
            if (!quest.getTargetMetric().equals(metric)) continue;
            QuestProgress progress = questProgressRepository.findByUserIdAndQuestId(userId, quest.getId()).orElseGet(() -> QuestProgress.builder().userId(userId).questId(quest.getId()).build());
            long increment = delta;
            if ("DISTINCT_CATEGORY_COUNT".equals(metric)) {
                QuestProgressCategory.Id id = new QuestProgressCategory.Id(userId, quest.getId(), categoryId);
                if (questProgressCategoryRepository.existsById(id)) continue;
                questProgressCategoryRepository.save(new QuestProgressCategory(userId, quest.getId(), categoryId));
                increment = 1;
            }
            progress.setCurrentValue(progress.getCurrentValue() + increment);
            if (progress.getCurrentValue() >= quest.getTargetValue() && progress.getCompletedAt() == null) {
                progress.setCompletedAt(LocalDateTime.now());
                enqueueQuestCompleted(userId, quest, progress.getCompletedAt());
            }
            questProgressRepository.save(progress);
        }
    }

    private void enqueueQuestCompleted(UUID userId, Quest quest, LocalDateTime completedAt) {
        UUID eventId = UUID.nameUUIDFromBytes(("quest:" + userId + ":" + quest.getId()).getBytes(StandardCharsets.UTF_8));
        QuestCompletedEvent event = QuestCompletedEvent.builder().eventId(eventId).userId(userId).questId(quest.getId())
                .rewardCoins(quest.getRewardCoins()).completedAt(completedAt.toInstant(java.time.ZoneOffset.UTC)).build();
        try {
            questRewardOutboxEventRepository.save(QuestRewardOutboxEvent.builder().eventId(eventId)
                    .eventType("QuestCompletedEvent").aggregateId(quest.getId())
                    .payload(objectMapper.writeValueAsString(event)).build());
        } catch (Exception exception) {
            throw new IllegalStateException("Could not persist quest completion outbox event", exception);
        }
    }

    private boolean matches(String rule, UserGamificationProfile profile) {
        try { JsonNode node = objectMapper.readTree(rule); return "GTE".equals(node.path("operator").asText()) && metric(profile, node.path("metric").asText()) >= node.path("value").asLong(); }
        catch (Exception ignored) { return false; }
    }
    private long metric(UserGamificationProfile p, String metric) { return switch (metric) {
        case "LISTINGS_PUBLISHED" -> p.getListingsPublished(); case "CONFIRMED_GIVE_COUNT" -> p.getConfirmedGiveCount(); case "CONFIRMED_TAKE_COUNT" -> p.getConfirmedTakeCount(); case "DISTINCT_CATEGORY_COUNT" -> p.getDistinctCategoryCount(); case "TOTAL_COINS_EARNED" -> p.getTotalCoinsEarned(); default -> -1; }; }

    @Override @Transactional(readOnly = true) public List<BadgeDto> getAllBadges() { return badgeRepository.findByActiveTrue().stream().map(this::badgeDto).collect(Collectors.toList()); }
    @Override @Transactional(readOnly = true) public List<UserBadgeDto> getUserBadges(UUID userId) { return userBadgeRepository.findByUserId(userId).stream().map(ub -> badgeRepository.findById(ub.getBadgeId()).map(b -> UserBadgeDto.builder().id(ub.getId()).userId(userId).badgeCode(b.getCode()).badgeName(b.getName()).description(b.getDescription()).iconKey(b.getIconKey()).earnedAt(ub.getEarnedAt()).build()).orElse(null)).filter(Objects::nonNull).toList(); }
    @Override @Transactional(readOnly = true) public UserLevelDto getUserLevel(UUID userId) { UserGamificationProfile p = profile(userId); return UserLevelDto.builder().userId(userId).level(p.getLevel()).totalPointsEarned(Math.toIntExact(p.getTotalCoinsEarned())).pointsToNextLevel(100 - (int)(p.getTotalCoinsEarned() % 100)).build(); }
    @Override @Transactional(readOnly = true) public List<QuestDto> getQuests(String period) { return questRepository.findByPeriod(period).stream().map(q -> QuestDto.builder().id(q.getId()).period(q.getPeriod()).code(q.getCode()).title(q.getTitle()).targetMetric(q.getTargetMetric()).targetValue(q.getTargetValue()).rewardCoins(q.getRewardCoins()).build()).toList(); }
    @Override @Transactional(readOnly = true) public List<LeaderboardDto> getLeaderboard(String period) { return leaderboardSnapshotRepository.findTopByScopeAndScopeIdIsNullAndPeriodOrderByGeneratedAtDesc("GLOBAL", period).map(snapshot -> leaderboardSnapshotRepository.findBySnapshotBatchIdOrderByRankAsc(snapshot.getSnapshotBatchId())).orElse(List.of()).stream().map(s -> LeaderboardDto.builder().userId(s.getUserId()).rank(s.getRank()).score(s.getScore()).scope(s.getScope()).period(s.getPeriod()).build()).toList(); }
    private BadgeDto badgeDto(Badge b) { return BadgeDto.builder().id(b.getId()).code(b.getCode()).name(b.getName()).description(b.getDescription()).iconKey(b.getIconKey()).build(); }
}
