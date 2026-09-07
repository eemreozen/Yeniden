package com.yeniden.ecocoin.repository;

import com.yeniden.ecocoin.domain.CoinEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * CoinEntry Entity'si için veritabanı erişim arayüzü.
 */
@Repository
public interface CoinEntryRepository extends JpaRepository<CoinEntry, UUID> {
    Page<CoinEntry> findByAccountOrderByCreatedAtDesc(String account, Pageable pageable);
    List<CoinEntry> findByTransactionId(UUID transactionId);

    @Query("select coalesce(sum(e.amount), 0) from CoinEntry e where e.account = :account")
    long balanceOfAccount(String account);

    @Query("select coalesce(sum(e.amount), 0) from CoinEntry e join CoinTransaction t on t.id = e.transactionId "
            + "where e.account = :account and t.type = com.yeniden.ecocoin.domain.TransactionType.GRANT and e.amount > 0")
    long totalGrantedToAccount(String account);

    @Query("select coalesce(sum(e.amount), 0) from CoinEntry e join CoinTransaction t on t.id = e.transactionId "
            + "where e.account = :account and t.type = com.yeniden.ecocoin.domain.TransactionType.GRANT and e.amount > 0 "
            + "and t.createdAt >= :from and t.reason <> 'QUEST_COMPLETED'")
    long handoverGrantedSince(String account, java.time.LocalDateTime from);

    @Query("select coalesce(sum(e.amount), 0) from CoinEntry e join CoinTransaction t on t.id = e.transactionId "
            + "where e.account = :account and t.type = com.yeniden.ecocoin.domain.TransactionType.GRANT and e.amount > 0 "
            + "and t.createdAt >= :from")
    long grantedSince(String account, java.time.LocalDateTime from);

    @Query("select e.account from CoinEntry e where e.account like 'USER:%' group by e.account having sum(e.amount) < 0")
    List<String> findNegativeUserAccounts();

    @Query("select coalesce(sum(e.amount), 0) from CoinEntry e")
    long globalLedgerSum();
}
