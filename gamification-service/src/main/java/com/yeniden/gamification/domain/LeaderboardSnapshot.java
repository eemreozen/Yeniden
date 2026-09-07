package com.yeniden.gamification.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity @Table(name = "leaderboard_snapshots", schema = "gamification", indexes = @Index(columnList = "scope,scope_id,period,rank"))
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class LeaderboardSnapshot {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(nullable = false) private String scope;
    private UUID scopeId;
    @Column(nullable = false, length = 7) private String period;
    @Column(name = "snapshot_batch_id", nullable = false) private UUID snapshotBatchId;
    @Column(nullable = false) private UUID userId;
    @Column(name = "rank", nullable = false) private int rank;
    @Column(nullable = false) private long score;
    @Column(nullable = false) private LocalDateTime generatedAt;
}
