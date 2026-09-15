package com.yeniden.ecocoin.repository;

import com.yeniden.ecocoin.domain.CoinTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

/**
 * CoinTransaction Entity'si için veritabanı erişim arayüzü.
 */
@Repository
public interface CoinTransactionRepository extends JpaRepository<CoinTransaction, UUID> {
    Optional<CoinTransaction> findByIdempotencyKey(String idempotencyKey);

    long countByTypeAndReasonAndBeneficiaryIdAndCounterpartyIdAndCreatedAtGreaterThanEqual(
            com.yeniden.ecocoin.domain.TransactionType type, String reason, UUID beneficiaryId, UUID counterpartyId,
            java.time.LocalDateTime from);

    long countByTypeAndReasonAndBeneficiaryIdAndCategoryIdAndCreatedAtGreaterThanEqual(
            com.yeniden.ecocoin.domain.TransactionType type, String reason, UUID beneficiaryId, UUID categoryId,
            java.time.LocalDateTime from);

    @Query("select t.id from CoinTransaction t join CoinEntry e on e.transactionId = t.id "
            + "group by t.id having sum(e.amount) <> 0")
    List<UUID> findUnbalancedTransactionIds();

    List<CoinTransaction> findByTypeAndCreatedAtBefore(com.yeniden.ecocoin.domain.TransactionType type,
                                                         java.time.LocalDateTime before);
}
