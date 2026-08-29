package com.yeniden.ecocoin.repository;

import com.yeniden.ecocoin.domain.CoinTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * CoinTransaction Entity'si için veritabanı erişim arayüzü.
 */
@Repository
public interface CoinTransactionRepository extends JpaRepository<CoinTransaction, UUID> {
    Optional<CoinTransaction> findByIdempotencyKey(String idempotencyKey);
}
