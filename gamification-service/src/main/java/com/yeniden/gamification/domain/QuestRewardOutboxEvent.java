package com.yeniden.gamification.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "quest_reward_outbox_events", schema = "gamification",
        uniqueConstraints = @UniqueConstraint(columnNames = "event_id"),
        indexes = @Index(name = "idx_quest_reward_outbox_pending", columnList = "published_at,created_at"))
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class QuestRewardOutboxEvent {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private UUID eventId;

    @Column(nullable = false)
    private String eventType;

    @Column(nullable = false)
    private UUID aggregateId;

    @Lob @Column(nullable = false)
    private String payload;

    @Builder.Default @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime publishedAt;
}
