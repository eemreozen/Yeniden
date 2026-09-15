package com.yeniden.gamification.service;

import com.yeniden.gamification.domain.LeaderboardSnapshot;
import com.yeniden.gamification.repository.LeaderboardSnapshotRepository;
import com.yeniden.gamification.repository.UserGamificationProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime; import java.time.YearMonth; import java.util.UUID; import java.util.concurrent.atomic.AtomicInteger;

@Component @RequiredArgsConstructor public class LeaderboardJob {
    private final UserGamificationProfileRepository profileRepository; private final LeaderboardSnapshotRepository snapshotRepository;
    @Scheduled(cron = "${gamification.leaderboard.cron:0 0 * * * *}") @Transactional public void generate() {
        AtomicInteger rank = new AtomicInteger();
        UUID batchId = UUID.randomUUID();
        LocalDateTime generatedAt = LocalDateTime.now();
        profileRepository.findAll().stream().sorted((a,b) -> Long.compare(b.getTotalCoinsEarned(), a.getTotalCoinsEarned())).forEach(p -> snapshotRepository.save(LeaderboardSnapshot.builder().scope("GLOBAL").period(YearMonth.now().toString()).snapshotBatchId(batchId).userId(p.getUserId()).rank(rank.incrementAndGet()).score(p.getTotalCoinsEarned()).generatedAt(generatedAt).build()));
    }
}
