package com.yeniden.ecocoin.service;

import com.yeniden.common.exception.BaseException;
import com.yeniden.ecocoin.domain.CoinEntry;
import com.yeniden.ecocoin.domain.CoinTransaction;
import com.yeniden.ecocoin.domain.TransactionType;
import com.yeniden.ecocoin.domain.Wallet;
import com.yeniden.ecocoin.dto.CoinEntryDto;
import com.yeniden.ecocoin.dto.GrantCoinsRequest;
import com.yeniden.ecocoin.dto.WalletDto;
import com.yeniden.ecocoin.repository.CoinEntryRepository;
import com.yeniden.ecocoin.repository.CoinTransactionRepository;
import com.yeniden.ecocoin.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * EcoCoin Service İş Mantığı Gerçekleşimi (04-ecocoin-rules.md).
 */
@Service
@RequiredArgsConstructor
public class EcoCoinServiceImpl implements EcoCoinService {

    private static final int DAILY_CAP = 100;
    private static final int MONTHLY_CAP = 1200;

    private final CoinTransactionRepository transactionRepository;
    private final CoinEntryRepository entryRepository;
    private final WalletRepository walletRepository;

    @Override
    @Transactional
    public WalletDto grantCoins(GrantCoinsRequest request) {
        // 1. Idempotency Kontrolü: Aynı işlem 2. kez gelirse tekrar puan verilmez!
        Optional<CoinTransaction> existingTx = transactionRepository.findByIdempotencyKey(request.getIdempotencyKey());
        if (existingTx.isPresent()) {
            return getWallet(request.getUserId());
        }

        // 2. Kullanıcı Cüzdanını Getir veya Oluştur
        Wallet wallet = walletRepository.findById(request.getUserId())
                .orElseGet(() -> Wallet.builder()
                        .userId(request.getUserId())
                        .balance(0)
                        .dailyEarnedToday(0)
                        .monthlyEarnedThisMonth(0)
                        .lastEarnedDate(LocalDate.now())
                        .build());

        // Gün / Ay Sıfırlama Kontrolü
        LocalDate today = LocalDate.now();
        if (wallet.getLastEarnedDate() == null || !wallet.getLastEarnedDate().isEqual(today)) {
            wallet.setDailyEarnedToday(0);
            if (wallet.getLastEarnedDate() != null && wallet.getLastEarnedDate().getMonth() != today.getMonth()) {
                wallet.setMonthlyEarnedThisMonth(0);
            }
            wallet.setLastEarnedDate(today);
        }

        // 3. Tavan Hesaplaması ( 04-ecocoin-rules.md )
        int dailyRemaining = Math.max(0, DAILY_CAP - wallet.getDailyEarnedToday());
        int monthlyRemaining = Math.max(0, MONTHLY_CAP - wallet.getMonthlyEarnedThisMonth());
        int allowedPoints = Math.min(request.getAmount(), Math.min(dailyRemaining, monthlyRemaining));

        if (allowedPoints > 0) {
            // 4. Double-Entry (Çift Kayıtlı Defter) İşlemi Oluştur
            CoinTransaction transaction = CoinTransaction.builder()
                    .idempotencyKey(request.getIdempotencyKey())
                    .type(TransactionType.GRANT)
                    .description(request.getDescription() != null ? request.getDescription() : "Teslimat Eco-Coin Ödülü")
                    .build();

            CoinTransaction savedTx = transactionRepository.save(transaction);

            // Entry 1: SYSTEM_MINT hesabı borçlanır (-)
            CoinEntry mintEntry = CoinEntry.builder()
                    .transactionId(savedTx.getId())
                    .account("SYSTEM_MINT")
                    .amount(-allowedPoints)
                    .build();

            // Entry 2: USER:{userId} hesabı alacaklanır (+)
            CoinEntry userEntry = CoinEntry.builder()
                    .transactionId(savedTx.getId())
                    .account("USER:" + request.getUserId())
                    .amount(allowedPoints)
                    .build();

            entryRepository.save(mintEntry);
            entryRepository.save(userEntry);

            // 5. Cüzdan Bakiyesini ve Tavan Sayaçlarını Güncelle
            wallet.setBalance(wallet.getBalance() + allowedPoints);
            wallet.setDailyEarnedToday(wallet.getDailyEarnedToday() + allowedPoints);
            wallet.setMonthlyEarnedThisMonth(wallet.getMonthlyEarnedThisMonth() + allowedPoints);
            walletRepository.save(wallet);
        }

        return mapToWalletDto(wallet);
    }

    @Override
    @Transactional(readOnly = true)
    public WalletDto getWallet(UUID userId) {
        Wallet wallet = walletRepository.findById(userId)
                .orElseGet(() -> Wallet.builder()
                        .userId(userId)
                        .balance(0)
                        .dailyEarnedToday(0)
                        .monthlyEarnedThisMonth(0)
                        .lastEarnedDate(LocalDate.now())
                        .build());

        return mapToWalletDto(wallet);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CoinEntryDto> getWalletEntries(UUID userId) {
        String account = "USER:" + userId;
        return entryRepository.findByAccountOrderByCreatedAtDesc(account)
                .stream()
                .map(this::mapToEntryDto)
                .collect(Collectors.toList());
    }

    private WalletDto mapToWalletDto(Wallet wallet) {
        int dailyRemaining = Math.max(0, DAILY_CAP - wallet.getDailyEarnedToday());
        int monthlyRemaining = Math.max(0, MONTHLY_CAP - wallet.getMonthlyEarnedThisMonth());

        return WalletDto.builder()
                .userId(wallet.getUserId())
                .balance(wallet.getBalance())
                .dailyEarnedToday(wallet.getDailyEarnedToday())
                .monthlyEarnedThisMonth(wallet.getMonthlyEarnedThisMonth())
                .dailyCapRemaining(dailyRemaining)
                .monthlyCapRemaining(monthlyRemaining)
                .build();
    }

    private CoinEntryDto mapToEntryDto(CoinEntry entry) {
        return CoinEntryDto.builder()
                .id(entry.getId())
                .transactionId(entry.getTransactionId())
                .account(entry.getAccount())
                .amount(entry.getAmount())
                .createdAt(entry.getCreatedAt())
                .build();
    }
}
