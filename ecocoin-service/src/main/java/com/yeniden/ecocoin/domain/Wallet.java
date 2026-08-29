package com.yeniden.ecocoin.domain;

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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * ecocoin.wallets tablosunun JPA Entity karşılığı. Kullanıcı bakiye ve tavan sayaçlarını tutar.
 */
@Entity
@Table(name = "wallets", schema = "ecocoin")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Wallet {

    @Id
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Builder.Default
    @Column(name = "balance", nullable = false)
    private int balance = 0;

    @Builder.Default
    @Column(name = "daily_earned_today", nullable = false)
    private int dailyEarnedToday = 0;

    @Builder.Default
    @Column(name = "monthly_earned_this_month", nullable = false)
    private int monthlyEarnedThisMonth = 0;

    @Column(name = "last_earned_date")
    private LocalDate lastEarnedDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.lastEarnedDate == null) {
            this.lastEarnedDate = LocalDate.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
