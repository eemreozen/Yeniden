package com.yeniden.ecocoin.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * ecocoin.entries tablosunun JPA Entity karşılığı. Çift kayıtlı defter (Double-Entry Ledger) satırıdır.
 */
@Entity
@Table(name = "entries", schema = "ecocoin")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoinEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "transaction_id", nullable = false)
    private UUID transactionId;

    @Column(name = "account", nullable = false, length = 100)
    private String account; // Örn: SYSTEM_MINT, USER:{userId}, SYSTEM_HOLD, SYSTEM_BURN

    @Column(name = "amount", nullable = false)
    private int amount; // Pozitif (+) alacak, Negatif (-) borç

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
