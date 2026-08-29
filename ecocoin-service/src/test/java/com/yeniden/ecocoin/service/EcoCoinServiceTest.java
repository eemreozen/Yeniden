package com.yeniden.ecocoin.service;

import com.yeniden.ecocoin.domain.CoinEntry;
import com.yeniden.ecocoin.domain.CoinTransaction;
import com.yeniden.ecocoin.domain.TransactionType;
import com.yeniden.ecocoin.domain.Wallet;
import com.yeniden.ecocoin.dto.GrantCoinsRequest;
import com.yeniden.ecocoin.dto.WalletDto;
import com.yeniden.ecocoin.repository.CoinEntryRepository;
import com.yeniden.ecocoin.repository.CoinTransactionRepository;
import com.yeniden.ecocoin.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EcoCoinServiceTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private CoinTransactionRepository transactionRepository;

    @Mock
    private CoinEntryRepository entryRepository;

    @InjectMocks
    private EcoCoinServiceImpl ecoCoinService;

    private UUID userId;
    private GrantCoinsRequest grantRequest;
    private Wallet wallet;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();

        grantRequest = GrantCoinsRequest.builder()
                .userId(userId)
                .amount(25)
                .idempotencyKey("TEST_KEY_123")
                .description("Teslimat Ödülü")
                .build();

        wallet = Wallet.builder()
                .userId(userId)
                .balance(50)
                .dailyEarnedToday(0)
                .monthlyEarnedThisMonth(0)
                .lastEarnedDate(LocalDate.now())
                .build();
    }

    @Test
    @DisplayName("Başarılı EcoCoin tanımlama ve Double-Entry Ledger kaydı")
    void grantCoins_Success() {
        when(transactionRepository.findByIdempotencyKey("TEST_KEY_123")).thenReturn(Optional.empty());
        when(walletRepository.findById(userId)).thenReturn(Optional.of(wallet));

        CoinTransaction mockTx = CoinTransaction.builder()
                .id(UUID.randomUUID())
                .type(TransactionType.GRANT)
                .idempotencyKey("TEST_KEY_123")
                .build();

        when(transactionRepository.save(any(CoinTransaction.class))).thenReturn(mockTx);

        WalletDto result = ecoCoinService.grantCoins(grantRequest);

        assertNotNull(result);
        assertEquals(75, result.getBalance()); // 50 + 25 = 75

        verify(transactionRepository).save(any(CoinTransaction.class));
        verify(entryRepository, times(2)).save(any(CoinEntry.class)); // 1 borç, 1 alacak kaydı
    }

    @Test
    @DisplayName("Aynı Idempotency Key iki kez çağrıldığında mükerrer ödül engellenmeli")
    void grantCoins_IdempotentDuplicate_ReturnsExisting() {
        CoinTransaction existingTx = CoinTransaction.builder()
                .id(UUID.randomUUID())
                .type(TransactionType.GRANT)
                .idempotencyKey("TEST_KEY_123")
                .build();

        when(transactionRepository.findByIdempotencyKey("TEST_KEY_123")).thenReturn(Optional.of(existingTx));
        when(walletRepository.findById(userId)).thenReturn(Optional.of(wallet));

        WalletDto result = ecoCoinService.grantCoins(grantRequest);

        assertNotNull(result);
        assertEquals(50, result.getBalance()); // Bakiye değişmez (50 kalır)
    }

    @Test
    @DisplayName("Günlük 100 EcoCoin tavanı dolduğunda ekstra puan verilmemeli (0 puan eklenir)")
    void grantCoins_ExceedsDailyCap_ClampsToZero() {
        wallet.setDailyEarnedToday(100); // Günlük tavan (100) doldu

        when(transactionRepository.findByIdempotencyKey("TEST_KEY_123")).thenReturn(Optional.empty());
        when(walletRepository.findById(userId)).thenReturn(Optional.of(wallet));

        WalletDto result = ecoCoinService.grantCoins(grantRequest);

        assertNotNull(result);
        assertEquals(50, result.getBalance()); // Günlük tavan dolduğu için bakiye artmaz
        verify(transactionRepository, never()).save(any(CoinTransaction.class));
    }
}
