package com.yeniden.exchange.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
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
 * exchange.handovers tablosunun JPA Entity karşılığı (ADR-005).
 */
@Entity
@Table(name = "handovers", schema = "exchange")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Handover {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "request_id", nullable = false)
    private UUID requestId;

    @Column(name = "listing_id", nullable = false)
    private UUID listingId;

    @Column(name = "provider_id", nullable = false)
    private UUID providerId;

    @Column(name = "receiver_id", nullable = false)
    private UUID receiverId;

    @Column(name = "confirmation_code_hash", nullable = false, length = 64)
    private String confirmationCodeHash;

    @Column(name = "raw_code_for_receiver", length = 6)
    private String rawCodeForReceiver; // Alıcı tarafa gösterilecek 6 haneli kod

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private HandoverStatus status;

    @Builder.Default
    @Column(name = "failed_attempts", nullable = false)
    private int failedAttempts = 0;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = HandoverStatus.PENDING_CODE;
        }
        if (this.expiresAt == null) {
            this.expiresAt = LocalDateTime.now().plusHours(72); // 72 saat geçerli
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
