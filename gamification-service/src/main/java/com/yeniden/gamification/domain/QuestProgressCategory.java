package com.yeniden.gamification.domain;

import jakarta.persistence.*;
import lombok.*;
import java.io.Serializable;
import java.util.UUID;

@Entity @Table(name = "quest_progress_categories", schema = "gamification") @Getter @NoArgsConstructor
public class QuestProgressCategory {
    @EmbeddedId private Id id;
    public QuestProgressCategory(UUID userId, UUID questId, UUID categoryId) { id = new Id(userId, questId, categoryId); }
    @Embeddable @Getter @EqualsAndHashCode @NoArgsConstructor @AllArgsConstructor
    public static class Id implements Serializable { private UUID userId; private UUID questId; private UUID categoryId; }
}
