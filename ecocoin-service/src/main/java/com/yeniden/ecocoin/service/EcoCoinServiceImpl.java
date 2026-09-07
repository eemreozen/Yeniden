package com.yeniden.ecocoin.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yeniden.common.event.CoinsGrantedEvent;
import com.yeniden.common.event.HandoverConfirmedEvent;
import com.yeniden.ecocoin.client.IdentityAccountAgeClient;
import com.yeniden.ecocoin.domain.CoinEntry;
import com.yeniden.ecocoin.domain.CoinTransaction;
import com.yeniden.ecocoin.domain.OutboxEvent;
import com.yeniden.ecocoin.domain.TransactionType;
import com.yeniden.ecocoin.domain.Wallet;
import com.yeniden.ecocoin.dto.CoinEntryDto;
import com.yeniden.ecocoin.dto.CoinGrantReason;
import com.yeniden.ecocoin.dto.CoinGrantResult;
import com.yeniden.ecocoin.dto.GrantCoinsRequest;
import com.yeniden.ecocoin.dto.WalletDto;
import com.yeniden.ecocoin.repository.CoinEntryRepository;
import com.yeniden.ecocoin.repository.CoinTransactionRepository;
import com.yeniden.ecocoin.repository.OutboxEventRepository;
import com.yeniden.ecocoin.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Instant;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EcoCoinServiceImpl implements EcoCoinService {

    private static final long DAILY_CAP = 100;
    private static final long MONTHLY_CAP = 1200;
    private static final String HANDOVER_REASON = "HANDOVER_CONFIRMED";
    private static final String QUEST_REASON = "QUEST_COMPLETED";

    private final CoinTransactionRepository transactionRepository;
    private final CoinEntryRepository entryRepository;
    private final WalletRepository walletRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;
    private final IdentityAccountAgeClient identityAccountAgeClient;

    @Override
    @Transactional
    public CoinGrantResult processHandover(HandoverConfirmedEvent event) {
        if (event.isReviewRequired() || "PENDING_REVIEW".equals(event.getHandoverStatus())) {
            return rejected(event.getProviderId(), CoinGrantReason.PENDING_REVIEW);
        }
        if (event.getCategoryId() == null || event.getCategoryCoinMultiplier() == null || event.getQuantityBand() == null) {
            return rejected(event.getProviderId(), CoinGrantReason.MISSING_REWARD_CONTEXT);
        }
        if (transactionRepository.findByIdempotencyKey("handover:" + event.getHandoverId()).isPresent()) {
            return rejected(event.getProviderId(), CoinGrantReason.DUPLICATE);
        }

        long reward = calculateHandoverReward(event.getCategoryCoinMultiplier(), event.getQuantityBand());
        if (reward <= 0) {
            return rejected(event.getProviderId(), CoinGrantReason.MISSING_REWARD_CONTEXT);
        }
        long dailyCap = effectiveDailyCapForGrant(event.getProviderId());
        return grant(event.getProviderId(), reward, "handover:" + event.getHandoverId(), HANDOVER_REASON,
                "handover:" + event.getHandoverId(), event.getReceiverId(), event.getCategoryId(), false, dailyCap);
    }

    @Override
    @Transactional
    public CoinGrantResult grantCoins(GrantCoinsRequest request) {
        if (!QUEST_REASON.equals(request.getReason())) {
            return rejected(request.getUserId(), CoinGrantReason.MISSING_REWARD_CONTEXT);
        }
        return grant(request.getUserId(), request.getAmount(), request.getIdempotencyKey(), QUEST_REASON,
                request.getSourceRef(), null, null, true, DAILY_CAP);
    }

    private CoinGrantResult grant(UUID userId, long amount, String idempotencyKey, String reason, String sourceRef,
                            UUID counterpartyId, UUID categoryId, boolean questReward, long dailyCap) {
        Optional<CoinTransaction> duplicate = transactionRepository.findByIdempotencyKey(idempotencyKey);
        if (duplicate.isPresent()) {
            return rejected(userId, CoinGrantReason.DUPLICATE);
        }
        if (amount <= 0) {
            return rejected(userId, CoinGrantReason.MISSING_REWARD_CONTEXT);
        }

        lockWallet(userId);
        String account = userAccount(userId);
        LocalDateTime now = LocalDateTime.now();
        long monthlyEarned = entryRepository.grantedSince(account, YearMonth.from(now).atDay(1).atStartOfDay());
        long dailyHandoverEarned = entryRepository.handoverGrantedSince(account, LocalDate.now().atStartOfDay());

        if (monthlyEarned + amount > MONTHLY_CAP) {
            return rejected(userId, CoinGrantReason.MONTHLY_CAP_REACHED);
        }
        if (!questReward && dailyHandoverEarned + amount > dailyCap) {
            return rejected(userId, CoinGrantReason.DAILY_CAP_REACHED);
        }
        if (!questReward && transactionRepository.countByTypeAndReasonAndBeneficiaryIdAndCounterpartyIdAndCreatedAtGreaterThanEqual(
                TransactionType.GRANT, HANDOVER_REASON, userId, counterpartyId, YearMonth.from(now).atDay(1).atStartOfDay()) >= 2) {
            return rejected(userId, CoinGrantReason.COUNTERPARTY_LIMIT_REACHED);
        }
        if (!questReward && transactionRepository.countByTypeAndReasonAndBeneficiaryIdAndCategoryIdAndCreatedAtGreaterThanEqual(
                TransactionType.GRANT, HANDOVER_REASON, userId, categoryId, LocalDate.now().atStartOfDay()) >= 3) {
            return rejected(userId, CoinGrantReason.CATEGORY_LIMIT_REACHED);
        }

        CoinTransaction transaction = transactionRepository.save(CoinTransaction.builder()
                .idempotencyKey(idempotencyKey).type(TransactionType.GRANT).reason(reason).sourceRef(sourceRef)
                .counterpartyId(counterpartyId).beneficiaryId(userId).categoryId(categoryId).createdBy("ecocoin-service").build());
        entryRepository.save(CoinEntry.builder().transactionId(transaction.getId()).account("SYSTEM_MINT").amount(-amount).build());
        entryRepository.save(CoinEntry.builder().transactionId(transaction.getId()).account(account).amount(amount).build());
        writeOutbox(transaction, userId, amount, reason, sourceRef);
        return CoinGrantResult.builder().granted(true).amount(amount).reason(CoinGrantReason.GRANTED)
                .wallet(walletDto(userId)).build();
    }

    private CoinGrantResult rejected(UUID userId, CoinGrantReason reason) {
        return CoinGrantResult.builder().granted(false).amount(0).reason(reason).wallet(walletDto(userId)).build();
    }

    private void lockWallet(UUID userId) {
        if (walletRepository.findByUserIdForUpdate(userId).isEmpty()) {
            walletRepository.saveAndFlush(Wallet.builder().userId(userId).build());
            walletRepository.findByUserIdForUpdate(userId);
        }
    }

    private void writeOutbox(CoinTransaction transaction, UUID userId, long amount, String reason, String sourceRef) {
        try {
            CoinsGrantedEvent event = CoinsGrantedEvent.builder().eventId(transaction.getId()).userId(userId)
                    .amount(Math.toIntExact(amount)).reason(reason).sourceHandoverId(parseHandoverId(sourceRef)).build();
            outboxEventRepository.save(OutboxEvent.builder().eventType("CoinsGrantedEvent")
                    .aggregateId(transaction.getId()).payload(objectMapper.writeValueAsString(event)).build());
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not serialize CoinsGrantedEvent", exception);
        }
    }

    private UUID parseHandoverId(String sourceRef) {
        if (sourceRef == null || !sourceRef.startsWith("handover:")) return null;
        return UUID.fromString(sourceRef.substring("handover:".length()));
    }

    private long calculateHandoverReward(BigDecimal categoryMultiplier, String quantityBand) {
        if (categoryMultiplier.compareTo(new BigDecimal("0.8")) < 0 || categoryMultiplier.compareTo(new BigDecimal("2.0")) > 0) {
            return 0;
        }
        BigDecimal quantityMultiplier = switch (quantityBand) {
            case "SINGLE" -> BigDecimal.ONE;
            case "FEW" -> new BigDecimal("1.5");
            case "MANY" -> new BigDecimal("2.0");
            default -> BigDecimal.ZERO;
        };
        return BigDecimal.TEN.multiply(categoryMultiplier).multiply(quantityMultiplier)
                .setScale(0, RoundingMode.HALF_UP).longValue();
    }

    @Override
    @Transactional(readOnly = true)
    public WalletDto getWallet(UUID userId) {
        return walletDto(userId);
    }

    private WalletDto walletDto(UUID userId) {
        String account = userAccount(userId);
        LocalDateTime now = LocalDateTime.now();
        long balance = entryRepository.balanceOfAccount(account);
        long totalEarned = entryRepository.totalGrantedToAccount(account);
        long dailyEarned = entryRepository.handoverGrantedSince(account, LocalDate.now().atStartOfDay());
        long monthlyEarned = entryRepository.grantedSince(account, YearMonth.from(now).atDay(1).atStartOfDay());
        long dailyCap = effectiveDailyCapForRead(userId).orElse(0);
        return WalletDto.builder().userId(userId).balance(balance).totalEarned(totalEarned)
                .dailyEarnedToday(dailyEarned).monthlyEarnedThisMonth(monthlyEarned)
                .dailyCapRemaining(Math.max(0, dailyCap - dailyEarned))
                .monthlyCapRemaining(Math.max(0, MONTHLY_CAP - monthlyEarned)).build();
    }

    private long effectiveDailyCapForGrant(UUID userId) {
        return dailyCap(identityAccountAgeClient.accountCreatedAtForGrant(userId));
    }

    private java.util.OptionalLong effectiveDailyCapForRead(UUID userId) {
        return identityAccountAgeClient.accountCreatedAt(userId)
                .map(this::dailyCap)
                .stream().mapToLong(Long::longValue).findFirst();
    }

    private long dailyCap(Instant createdAt) {
        return createdAt.plus(java.time.Duration.ofDays(7)).isAfter(Instant.now()) ? 30L : DAILY_CAP;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CoinEntryDto> getWalletEntries(UUID userId, Pageable pageable) {
        return entryRepository.findByAccountOrderByCreatedAtDesc(userAccount(userId), pageable).map(entry ->
                CoinEntryDto.builder().id(entry.getId()).transactionId(entry.getTransactionId())
                        .account(entry.getAccount()).amount(entry.getAmount()).createdAt(entry.getCreatedAt()).build());
    }

    private String userAccount(UUID userId) {
        return "USER:" + userId;
    }
}
