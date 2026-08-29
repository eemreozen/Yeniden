package com.yeniden.ecocoin.repository;

import com.yeniden.ecocoin.domain.CoinEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * CoinEntry Entity'si için veritabanı erişim arayüzü.
 */
@Repository
public interface CoinEntryRepository extends JpaRepository<CoinEntry, UUID> {
    List<CoinEntry> findByAccountOrderByCreatedAtDesc(String account);
    List<CoinEntry> findByTransactionId(UUID transactionId);
}
