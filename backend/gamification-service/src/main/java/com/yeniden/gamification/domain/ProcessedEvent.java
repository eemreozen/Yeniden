package com.yeniden.gamification.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity @Table(name = "processed_events", schema = "gamification") @Getter @NoArgsConstructor
public class ProcessedEvent {
    @EmbeddedId private ProcessedEventId id;
    @Column(nullable = false, updatable = false) private LocalDateTime processedAt;
    public ProcessedEvent(String listener, UUID eventId) { this.id = new ProcessedEventId(listener, eventId); }
    @PrePersist void created() { processedAt = LocalDateTime.now(); }

    @Embeddable @Getter @EqualsAndHashCode @NoArgsConstructor @AllArgsConstructor
    public static class ProcessedEventId implements Serializable {
        private String listener;
        private UUID eventId;
    }
}
