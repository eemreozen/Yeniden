package com.yeniden.gamification.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity @Table(name = "quest_progress", schema = "gamification", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "quest_id"}))
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class QuestProgress {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(nullable = false) private UUID userId;
    @Column(nullable = false) private UUID questId;
    @Builder.Default @Column(nullable = false) private long currentValue = 0;
    private LocalDateTime completedAt;
}
