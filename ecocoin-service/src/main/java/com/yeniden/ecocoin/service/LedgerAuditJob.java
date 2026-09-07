package com.yeniden.ecocoin.service;

import com.yeniden.ecocoin.domain.TransactionType;
import com.yeniden.ecocoin.repository.CoinEntryRepository;
import com.yeniden.ecocoin.repository.CoinTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class LedgerAuditJob {
    private final CoinTransactionRepository transactionRepository;
    private final CoinEntryRepository entryRepository;

    @Scheduled(cron = "${ecocoin.ledger-audit.cron:0 0 2 * * *}")
    @Transactional(readOnly = true)
    public void auditLedger() {
        List<UUID> unbalanced = transactionRepository.findUnbalancedTransactionIds();
        long globalSum = entryRepository.globalLedgerSum();
        List<String> negativeAccounts = entryRepository.findNegativeUserAccounts();
        List<UUID> staleHolds = transactionRepository.findByTypeAndCreatedAtBefore(
                        TransactionType.HOLD, LocalDateTime.now().minusMinutes(15))
                .stream().map(transaction -> transaction.getId()).toList();
        if (!unbalanced.isEmpty() || globalSum != 0 || !negativeAccounts.isEmpty() || !staleHolds.isEmpty()) {
            log.error("Ledger audit failed. unbalanced={}, globalSum={}, negativeAccounts={}, staleHolds={}",
                    unbalanced, globalSum, negativeAccounts, staleHolds);
        }
    }
}
