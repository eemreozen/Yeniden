package com.yeniden.gamification.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yeniden.common.event.CoinsGrantedEvent;
import com.yeniden.common.event.HandoverConfirmedEvent;
import com.yeniden.gamification.domain.*;
import com.yeniden.gamification.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import java.math.BigDecimal;
import java.util.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GamificationServiceTest {
    @Mock BadgeRepository badges; @Mock UserBadgeRepository userBadges; @Mock UserLevelRepository levels;
    @Mock UserGamificationProfileRepository profiles; @Mock ProcessedEventRepository processed;
    @Mock UserCategoryRepository categories; @Mock QuestRepository quests; @Mock QuestProgressRepository progress;
    @Mock QuestProgressCategoryRepository questCategories;
    @Mock LeaderboardSnapshotRepository snapshots;
    @Mock QuestRewardOutboxEventRepository questRewardOutbox;
    GamificationServiceImpl service; UUID userId; UserGamificationProfile profile;

    @BeforeEach void setUp() {
        userId = UUID.randomUUID(); profile = UserGamificationProfile.builder().userId(userId).build();
        service = new GamificationServiceImpl(badges, userBadges, levels, profiles, processed, categories, quests, progress, questCategories, snapshots, mapper(), questRewardOutbox);
        when(profiles.findById(userId)).thenReturn(Optional.of(profile));
        when(processed.existsById(any())).thenReturn(false);
        when(badges.findByActiveTrue()).thenReturn(List.of()); when(quests.findByPeriod(anyString())).thenReturn(List.of());
    }

    @Test void coinsGrantedIncreasesLifetimeEarnedAndLevelOnce() {
        CoinsGrantedEvent event = CoinsGrantedEvent.builder().eventId(UUID.randomUUID()).userId(userId).amount(150).build();
        when(processed.existsById(any())).thenReturn(false, true);
        service.handleCoinsGranted(event); service.handleCoinsGranted(event);
        assertEquals(150, profile.getTotalCoinsEarned()); assertEquals(2, profile.getLevel());
        verify(profiles, times(1)).save(profile);
    }

    @Test void badgeRuleAwardsOnceWhenMetricMatches() {
        Badge badge = Badge.builder().id(UUID.randomUUID()).code("COIN_100").name("Coins").active(true)
                .rule("{\"metric\":\"TOTAL_COINS_EARNED\",\"operator\":\"GTE\",\"value\":100}").build();
        when(badges.findByActiveTrue()).thenReturn(List.of(badge)); when(userBadges.findByUserIdAndBadgeId(userId, badge.getId())).thenReturn(Optional.empty(), Optional.of(UserBadge.builder().build()));
        service.handleCoinsGranted(CoinsGrantedEvent.builder().eventId(UUID.randomUUID()).userId(userId).amount(100).build());
        service.handleCoinsGranted(CoinsGrantedEvent.builder().eventId(UUID.randomUUID()).userId(userId).amount(1).build());
        verify(userBadges, times(1)).save(any(UserBadge.class));
    }

    @Test void badgeRuleDoesNotAwardWhenMetricFails() {
        Badge badge = Badge.builder().id(UUID.randomUUID()).code("COIN_100").name("Coins").active(true)
                .rule("{\"metric\":\"TOTAL_COINS_EARNED\",\"operator\":\"GTE\",\"value\":100}").build();
        when(badges.findByActiveTrue()).thenReturn(List.of(badge));
        service.handleCoinsGranted(CoinsGrantedEvent.builder().eventId(UUID.randomUUID()).userId(userId).amount(99).build());
        verify(userBadges, never()).save(any());
    }

    @Test void handoverUpdatesGiveTakeAndCategoryCounters() {
        UUID receiver = UUID.randomUUID(); UserGamificationProfile receiverProfile = UserGamificationProfile.builder().userId(receiver).build();
        when(profiles.findById(receiver)).thenReturn(Optional.of(receiverProfile)); when(categories.existsById(any())).thenReturn(false);
        service.handleHandoverConfirmed(HandoverConfirmedEvent.builder().handoverId(UUID.randomUUID()).providerId(userId).receiverId(receiver).categoryId(UUID.randomUUID()).categoryCoinMultiplier(BigDecimal.ONE).quantityBand("SINGLE").build());
        assertEquals(1, profile.getConfirmedGiveCount()); assertEquals(1, receiverProfile.getConfirmedTakeCount()); assertEquals(1, profile.getDistinctCategoryCount());
    }

    @Test void handoverWithoutCategoryContextUpdatesCountsSafely() {
        UUID receiver = UUID.randomUUID();
        when(profiles.findById(receiver)).thenReturn(Optional.of(UserGamificationProfile.builder().userId(receiver).build()));
        service.handleHandoverConfirmed(HandoverConfirmedEvent.builder().handoverId(UUID.randomUUID()).providerId(userId).receiverId(receiver).build());
        assertEquals(1, profile.getConfirmedGiveCount()); assertEquals(0, profile.getDistinctCategoryCount());
    }

    @Test void questProgressCompletesOnce() {
        Quest quest = Quest.builder().id(UUID.randomUUID()).period(java.time.YearMonth.now().toString()).code("C100").title("Coins").targetMetric("TOTAL_COINS_EARNED").targetValue(100).rewardCoins(10).build();
        QuestProgress qp = QuestProgress.builder().userId(userId).questId(quest.getId()).build();
        when(quests.findByPeriod(anyString())).thenReturn(List.of(quest)); when(progress.findByUserIdAndQuestId(userId, quest.getId())).thenReturn(Optional.of(qp));
        service.handleCoinsGranted(CoinsGrantedEvent.builder().eventId(UUID.randomUUID()).userId(userId).amount(100).build());
        assertEquals(100, qp.getCurrentValue()); org.junit.jupiter.api.Assertions.assertNotNull(qp.getCompletedAt());
        ArgumentCaptor<QuestRewardOutboxEvent> outbox = ArgumentCaptor.forClass(QuestRewardOutboxEvent.class);
        verify(questRewardOutbox).save(outbox.capture());
        assertEquals("QuestCompletedEvent", outbox.getValue().getEventType());
    }

    @Test void monthlyQuestStartsFromItsOwnProgressNotLifetimeCounter() {
        profile.setConfirmedGiveCount(10);
        Quest quest = Quest.builder().id(UUID.randomUUID()).period(java.time.YearMonth.now().toString()).code("G3").title("Give").targetMetric("CONFIRMED_GIVE_COUNT").targetValue(3).rewardCoins(10).build();
        QuestProgress qp = QuestProgress.builder().userId(userId).questId(quest.getId()).currentValue(0).build();
        when(quests.findByPeriod(anyString())).thenReturn(List.of(quest)); when(progress.findByUserIdAndQuestId(userId, quest.getId())).thenReturn(Optional.of(qp));
        UUID receiver = UUID.randomUUID(); when(profiles.findById(receiver)).thenReturn(Optional.of(UserGamificationProfile.builder().userId(receiver).build()));
        service.handleHandoverConfirmed(HandoverConfirmedEvent.builder().handoverId(UUID.randomUUID()).providerId(userId).receiverId(receiver).build());
        assertEquals(1, qp.getCurrentValue());
    }

    @Test void questProgressDoesNotAdvanceForQuestInAnotherPeriod() {
        Quest oldQuest = Quest.builder().id(UUID.randomUUID()).period("2000-01").code("OLD").title("Old").targetMetric("TOTAL_COINS_EARNED").targetValue(1).rewardCoins(1).build();
        when(quests.findByPeriod(java.time.YearMonth.now().toString())).thenReturn(List.of());
        service.handleCoinsGranted(CoinsGrantedEvent.builder().eventId(UUID.randomUUID()).userId(userId).amount(10).build());
        verify(progress, never()).findByUserIdAndQuestId(userId, oldQuest.getId());
    }

    @Test void duplicateEventDoesNotIncrementQuestProgressTwice() {
        Quest quest = Quest.builder().id(UUID.randomUUID()).period(java.time.YearMonth.now().toString()).code("C1").title("Coin").targetMetric("TOTAL_COINS_EARNED").targetValue(2).rewardCoins(1).build();
        QuestProgress qp = QuestProgress.builder().userId(userId).questId(quest.getId()).build();
        when(quests.findByPeriod(anyString())).thenReturn(List.of(quest)); when(progress.findByUserIdAndQuestId(userId, quest.getId())).thenReturn(Optional.of(qp));
        when(processed.existsById(any())).thenReturn(false, true);
        CoinsGrantedEvent event = CoinsGrantedEvent.builder().eventId(UUID.randomUUID()).userId(userId).amount(1).build();
        service.handleCoinsGranted(event); service.handleCoinsGranted(event);
        assertEquals(1, qp.getCurrentValue());
    }

    @Test void questRewardCoinsDoNotAdvanceCoinQuestProgress() {
        Quest quest = Quest.builder().id(UUID.randomUUID()).period(java.time.YearMonth.now().toString()).code("C1").title("Coin").targetMetric("TOTAL_COINS_EARNED").targetValue(1).rewardCoins(1).build();
        when(quests.findByPeriod(anyString())).thenReturn(List.of(quest));
        service.handleCoinsGranted(CoinsGrantedEvent.builder().eventId(UUID.randomUUID()).userId(userId).amount(10).reason("QUEST_COMPLETED").build());
        verify(progress, never()).findByUserIdAndQuestId(userId, quest.getId());
    }

    @Test void leaderboardSnapshotGenerationUsesOwnedProfiles() {
        UserGamificationProfile second = UserGamificationProfile.builder().userId(UUID.randomUUID()).totalCoinsEarned(20).build(); profile.setTotalCoinsEarned(50);
        when(profiles.findAll()).thenReturn(List.of(second, profile));
        new LeaderboardJob(profiles, snapshots).generate();
        ArgumentCaptor<LeaderboardSnapshot> saved = ArgumentCaptor.forClass(LeaderboardSnapshot.class);
        verify(snapshots, times(2)).save(saved.capture());
        assertEquals(saved.getAllValues().get(0).getSnapshotBatchId(), saved.getAllValues().get(1).getSnapshotBatchId());
    }

    @Test void leaderboardReadUsesOnlyLatestSnapshotBatch() {
        UUID latest = UUID.randomUUID();
        LeaderboardSnapshot marker = LeaderboardSnapshot.builder().snapshotBatchId(latest).build();
        when(snapshots.findTopByScopeAndScopeIdIsNullAndPeriodOrderByGeneratedAtDesc("GLOBAL", "2026-09")).thenReturn(Optional.of(marker));
        when(snapshots.findBySnapshotBatchIdOrderByRankAsc(latest)).thenReturn(List.of(LeaderboardSnapshot.builder().snapshotBatchId(latest).userId(userId).rank(1).score(10).scope("GLOBAL").period("2026-09").build()));
        assertEquals(1, service.getLeaderboard("2026-09").size());
        verify(snapshots, never()).findBySnapshotBatchIdOrderByRankAsc(UUID.randomUUID());
    }

    private ObjectMapper mapper() {
        return new ObjectMapper().findAndRegisterModules();
    }
}
