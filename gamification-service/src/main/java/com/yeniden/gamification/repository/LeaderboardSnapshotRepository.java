package com.yeniden.gamification.repository;
import com.yeniden.gamification.domain.LeaderboardSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List; import java.util.Optional; import java.util.UUID;
@Repository public interface LeaderboardSnapshotRepository extends JpaRepository<LeaderboardSnapshot, UUID> {
    Optional<LeaderboardSnapshot> findTopByScopeAndScopeIdIsNullAndPeriodOrderByGeneratedAtDesc(String scope, String period);
    List<LeaderboardSnapshot> findBySnapshotBatchIdOrderByRankAsc(UUID snapshotBatchId);
}
