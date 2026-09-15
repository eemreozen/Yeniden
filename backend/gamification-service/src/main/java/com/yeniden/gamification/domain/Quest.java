package com.yeniden.gamification.domain;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity @Table(name = "quests", schema = "gamification", uniqueConstraints = @UniqueConstraint(columnNames = {"period", "code"}))
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Quest {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(nullable = false, length = 7) private String period;
    @Column(nullable = false) private String code;
    @Column(nullable = false) private String title;
    @Column(nullable = false) private String targetMetric;
    @Column(nullable = false) private long targetValue;
    @Column(nullable = false) private long rewardCoins;
}
