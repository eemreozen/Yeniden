package com.yeniden.ecocoin.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yeniden.common.event.HandoverConfirmedEvent;
import com.yeniden.common.event.CoinsGrantedEvent;
import com.yeniden.ecocoin.config.RabbitMQConfig;
import com.yeniden.ecocoin.client.IdentityAccountAgeClient;
import com.yeniden.ecocoin.client.AccountAgeUnavailableException;
import com.yeniden.ecocoin.domain.CoinEntry;
import com.yeniden.ecocoin.domain.CoinTransaction;
import com.yeniden.ecocoin.domain.OutboxEvent;
import com.yeniden.ecocoin.domain.Wallet;
import com.yeniden.ecocoin.dto.GrantCoinsRequest;
import com.yeniden.ecocoin.dto.WalletDto;
import com.yeniden.ecocoin.dto.CoinGrantResult;
import com.yeniden.ecocoin.dto.CoinGrantReason;
import com.yeniden.ecocoin.repository.CoinEntryRepository;
import com.yeniden.ecocoin.repository.CoinTransactionRepository;
import com.yeniden.ecocoin.repository.OutboxEventRepository;
import com.yeniden.ecocoin.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Instant;
import java.time.Duration;
import java.util.Optional;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EcoCoinServiceTest {
    @Mock private WalletRepository walletRepository;
    @Mock private CoinTransactionRepository transactionRepository;
    @Mock private CoinEntryRepository entryRepository;
    @Mock private OutboxEventRepository outboxEventRepository;
    @Mock private RabbitTemplate rabbitTemplate;
    @Mock private IdentityAccountAgeClient identityAccountAgeClient;
    private EcoCoinServiceImpl service;
    private UUID userId;
    private UUID handoverId;
    private HandoverConfirmedEvent event;

    @BeforeEach
    void setUp() {
        service = new EcoCoinServiceImpl(transactionRepository, entryRepository, walletRepository,
                outboxEventRepository, new ObjectMapper(), identityAccountAgeClient);
        userId = UUID.randomUUID();
        handoverId = UUID.randomUUID();
        event = HandoverConfirmedEvent.builder().handoverId(handoverId).providerId(userId)
                .receiverId(UUID.randomUUID()).categoryId(UUID.randomUUID())
                .categoryCoinMultiplier(new BigDecimal("1.2")).quantityBand("FEW").build();
        when(walletRepository.findByUserIdForUpdate(userId)).thenReturn(Optional.of(Wallet.builder().userId(userId).build()));
        when(transactionRepository.findByIdempotencyKey(anyString())).thenReturn(Optional.empty());
        when(identityAccountAgeClient.accountCreatedAt(userId)).thenReturn(Optional.of(Instant.now().minus(Duration.ofDays(8))));
        when(identityAccountAgeClient.accountCreatedAtForGrant(userId)).thenReturn(Instant.now().minus(Duration.ofDays(8)));
        stubLedger(0, 0, 0, 0);
    }

    @Test
    void fullGrantCreatesBalancedEntriesAndOutbox() {
        UUID transactionId = UUID.randomUUID();
        when(transactionRepository.save(any())).thenReturn(CoinTransaction.builder().id(transactionId).build());
        CoinGrantResult result = service.processHandover(event);
        assertEquals(true, result.isGranted());
        assertEquals(18, result.getAmount());
        assertEquals(CoinGrantReason.GRANTED, result.getReason());
        ArgumentCaptor<CoinEntry> entries = ArgumentCaptor.forClass(CoinEntry.class);
        verify(entryRepository, times(2)).save(entries.capture());
        assertEquals(0, entries.getAllValues().stream().mapToLong(CoinEntry::getAmount).sum());
        assertEquals(-18, entries.getAllValues().get(0).getAmount());
        assertEquals(18, entries.getAllValues().get(1).getAmount());
        verify(outboxEventRepository).save(any(OutboxEvent.class));
    }

    @Test
    void capOverflowCreatesNoPartialGrant() {
        stubLedger(0, 90, 0, 0);
        CoinGrantResult result = service.processHandover(event);
        assertEquals(CoinGrantReason.DAILY_CAP_REACHED, result.getReason());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void dailyCapPreventsGrant() {
        stubLedger(0, 100, 0, 0);
        CoinGrantResult result = service.processHandover(event);
        assertEquals(CoinGrantReason.DAILY_CAP_REACHED, result.getReason());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void monthlyCapPreventsGrant() {
        stubLedger(0, 0, 1190, 0);
        CoinGrantResult result = service.processHandover(event);
        assertEquals(CoinGrantReason.MONTHLY_CAP_REACHED, result.getReason());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void counterpartyMonthlyLimitPreventsGrant() {
        when(transactionRepository.countByTypeAndReasonAndBeneficiaryIdAndCounterpartyIdAndCreatedAtGreaterThanEqual(any(), anyString(), any(), any(), any(LocalDateTime.class))).thenReturn(2L);
        CoinGrantResult result = service.processHandover(event);
        assertEquals(CoinGrantReason.COUNTERPARTY_LIMIT_REACHED, result.getReason());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void categoryDailyLimitPreventsGrant() {
        when(transactionRepository.countByTypeAndReasonAndBeneficiaryIdAndCategoryIdAndCreatedAtGreaterThanEqual(any(), anyString(), any(), any(), any(LocalDateTime.class))).thenReturn(3L);
        CoinGrantResult result = service.processHandover(event);
        assertEquals(CoinGrantReason.CATEGORY_LIMIT_REACHED, result.getReason());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void reviewRequiredHandoverDoesNotGrant() {
        event.setReviewRequired(true);
        CoinGrantResult result = service.processHandover(event);
        assertEquals(CoinGrantReason.PENDING_REVIEW, result.getReason());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void duplicateIdempotencyDoesNotGrantTwice() {
        when(transactionRepository.findByIdempotencyKey("handover:" + handoverId)).thenReturn(Optional.of(CoinTransaction.builder().id(UUID.randomUUID()).build()));
        CoinGrantResult result = service.processHandover(event);
        assertEquals(CoinGrantReason.DUPLICATE, result.getReason());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void walletBalanceAndTotalEarnedAreLedgerDerived() {
        stubLedger(45, 20, 15, 80);
        WalletDto wallet = service.getWallet(userId);
        assertEquals(45, wallet.getBalance());
        assertEquals(80, wallet.getTotalEarned());
        assertEquals(20, wallet.getDailyEarnedToday());
        assertEquals(15, wallet.getMonthlyEarnedThisMonth());
    }

    @Test
    void newAccountWalletUsesThirtyCoinDailyCap() {
        stubLedger(0, 20, 20, 20);
        when(identityAccountAgeClient.accountCreatedAt(userId)).thenReturn(Optional.of(Instant.now().minus(Duration.ofDays(6))));

        WalletDto wallet = service.getWallet(userId);

        assertEquals(10, wallet.getDailyCapRemaining());
    }

    @Test
    void olderAccountWalletUsesOneHundredCoinDailyCap() {
        stubLedger(0, 20, 20, 20);
        when(identityAccountAgeClient.accountCreatedAt(userId)).thenReturn(Optional.of(Instant.now().minus(Duration.ofDays(8))));

        WalletDto wallet = service.getWallet(userId);

        assertEquals(80, wallet.getDailyCapRemaining());
    }

    @Test
    void walletDoesNotAssumeNormalCapWhenAccountAgeUnavailable() {
        stubLedger(0, 20, 20, 20);
        when(identityAccountAgeClient.accountCreatedAt(userId)).thenReturn(Optional.empty());

        WalletDto wallet = service.getWallet(userId);

        assertEquals(0, wallet.getDailyCapRemaining());
    }

    @Test
    void missingRewardContextIsRejected() {
        event.setCategoryId(null);
        CoinGrantResult result = service.processHandover(event);
        assertEquals(false, result.isGranted());
        assertEquals(CoinGrantReason.MISSING_REWARD_CONTEXT, result.getReason());
    }

    @Test
    void accountYoungerThanSevenDaysUsesDailyCapThirty() {
        when(identityAccountAgeClient.accountCreatedAt(userId)).thenReturn(Optional.of(Instant.now().minus(Duration.ofDays(6))));
        when(identityAccountAgeClient.accountCreatedAtForGrant(userId)).thenReturn(Instant.now().minus(Duration.ofDays(6)));
        when(transactionRepository.save(any())).thenReturn(CoinTransaction.builder().id(UUID.randomUUID()).build());

        CoinGrantResult result = service.processHandover(event);

        assertEquals(true, result.isGranted());
        assertEquals(18, result.getAmount());
    }

    @Test
    void accountAtLeastSevenDaysUsesDailyCapOneHundred() {
        stubLedger(0, 20, 0, 0);
        when(identityAccountAgeClient.accountCreatedAt(userId)).thenReturn(Optional.of(Instant.now().minus(Duration.ofDays(7))));
        when(identityAccountAgeClient.accountCreatedAtForGrant(userId)).thenReturn(Instant.now().minus(Duration.ofDays(7)));
        when(transactionRepository.save(any())).thenReturn(CoinTransaction.builder().id(UUID.randomUUID()).build());

        CoinGrantResult result = service.processHandover(event);

        assertEquals(true, result.isGranted());
    }

    @Test
    void newAccountOverThirtyDailyCapIsRejectedWithoutPartialGrant() {
        stubLedger(0, 20, 0, 0);
        when(identityAccountAgeClient.accountCreatedAt(userId)).thenReturn(Optional.of(Instant.now().minus(Duration.ofDays(6))));
        when(identityAccountAgeClient.accountCreatedAtForGrant(userId)).thenReturn(Instant.now().minus(Duration.ofDays(6)));

        CoinGrantResult result = service.processHandover(event);

        assertEquals(0, result.getAmount());
        assertEquals(CoinGrantReason.DAILY_CAP_REACHED, result.getReason());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void accountAgeUnavailableIsRetryableAndCreatesNoLedgerState() {
        when(identityAccountAgeClient.accountCreatedAt(userId)).thenReturn(Optional.empty());
        when(identityAccountAgeClient.accountCreatedAtForGrant(userId)).thenThrow(new AccountAgeUnavailableException(userId));

        assertThrows(AccountAgeUnavailableException.class, () -> service.processHandover(event));

        verify(transactionRepository, never()).save(any());
        verify(entryRepository, never()).save(any());
        verify(outboxEventRepository, never()).save(any());
    }

    @Test
    void walletEntriesArePaginated() {
        CoinEntry entry = CoinEntry.builder().id(UUID.randomUUID()).transactionId(UUID.randomUUID())
                .account("USER:" + userId).amount(10).build();
        PageRequest pageRequest = PageRequest.of(1, 5);
        when(entryRepository.findByAccountOrderByCreatedAtDesc("USER:" + userId, pageRequest))
                .thenReturn(new PageImpl<>(List.of(entry), pageRequest, 6));

        Page<com.yeniden.ecocoin.dto.CoinEntryDto> page = service.getWalletEntries(userId, pageRequest);

        assertEquals(1, page.getNumber());
        assertEquals(5, page.getSize());
        assertEquals(6, page.getTotalElements());
        assertEquals(10, page.getContent().getFirst().getAmount());
    }

    @Test
    void questBypassesDailyCapButNotMonthlyCap() {
        // The handover daily total is already at the new-account limit; quests still only use the monthly cap.
        stubLedger(0, 30, 1150, 0);
        when(transactionRepository.save(any())).thenReturn(CoinTransaction.builder().id(UUID.randomUUID()).build());
        service.grantCoins(quest(50, "quest-1"));
        verify(transactionRepository).save(any());
        stubLedger(0, 30, 1180, 0);
        service.grantCoins(quest(50, "quest-2"));
        verify(transactionRepository, times(1)).save(any());
    }

    @Test
    void outboxPublisherPublishesPendingEvent() throws Exception {
        OutboxEvent outboxEvent = OutboxEvent.builder().id(UUID.randomUUID()).aggregateId(UUID.randomUUID())
                .eventType("CoinsGrantedEvent").payload(new ObjectMapper().writeValueAsString(CoinsGrantedEvent.builder()
                        .eventId(UUID.randomUUID()).userId(userId).amount(18).reason("HANDOVER_CONFIRMED").build())).build();
        when(outboxEventRepository.findTop100ByPublishedAtIsNullOrderByCreatedAtAsc()).thenReturn(List.of(outboxEvent));

        new OutboxPublisher(outboxEventRepository, rabbitTemplate, new ObjectMapper()).publishPendingEvents();

        verify(rabbitTemplate).convertAndSend(org.mockito.ArgumentMatchers.eq(RabbitMQConfig.COINS_GRANTED_EXCHANGE),
                org.mockito.ArgumentMatchers.eq(RabbitMQConfig.COINS_GRANTED_ROUTING_KEY), org.mockito.ArgumentMatchers.any(CoinsGrantedEvent.class));
        org.junit.jupiter.api.Assertions.assertNotNull(outboxEvent.getPublishedAt());
    }

    private GrantCoinsRequest quest(long amount, String source) {
        return GrantCoinsRequest.builder().userId(userId).amount(amount).reason("QUEST_COMPLETED")
                .idempotencyKey("quest:" + userId + ":" + source).sourceRef("quest:" + source).build();
    }

    private void stubLedger(long balance, long daily, long monthly, long total) {
        when(entryRepository.balanceOfAccount(anyString())).thenReturn(balance);
        when(entryRepository.handoverGrantedSince(anyString(), any(LocalDateTime.class))).thenReturn(daily);
        when(entryRepository.grantedSince(anyString(), any(LocalDateTime.class))).thenReturn(monthly);
        when(entryRepository.totalGrantedToAccount(anyString())).thenReturn(total);
    }
}
