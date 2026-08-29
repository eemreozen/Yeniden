package com.yeniden.identity.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Kullanıcının teslimat geçmişinden ve davranışlarından türetilen Güven Skoru (0-100) tablosu.
 */
@Entity
@Table(name = "trust_scores", schema = "identity")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrustScore {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Builder.Default
    @Column(name = "score", nullable = false)
    private int score = 50;

    @Builder.Default
    @Column(name = "completed_handovers", nullable = false)
    private int completedHandovers = 0;

    @Builder.Default
    @Column(name = "no_show_count", nullable = false)
    private int noShowCount = 0;

    @Builder.Default
    @Column(name = "report_count", nullable = false)
    private int reportCount = 0;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void onSave() {
        this.updatedAt = LocalDateTime.now();
        // Skoru 0 ile 100 arasında sınırla
        if (this.score < 0) this.score = 0;
        if (this.score > 100) this.score = 100;
    }
}
