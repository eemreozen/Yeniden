package com.yeniden.exchange.service;

import com.yeniden.common.exception.BaseException;
import com.yeniden.exchange.client.CatalogRewardContextClient;
import com.yeniden.exchange.client.ListingRewardContext;
import com.yeniden.exchange.domain.Handover;
import com.yeniden.exchange.domain.HandoverOutboxEvent;
import com.yeniden.exchange.domain.HandoverStatus;
import com.yeniden.exchange.domain.ListingRequest;
import com.yeniden.exchange.domain.RequestStatus;
import com.yeniden.exchange.dto.ConfirmCodeRequest;
import com.yeniden.exchange.dto.CreateRequestDto;
import com.yeniden.exchange.dto.HandoverDto;
import com.yeniden.exchange.dto.ListingRequestDto;
import com.yeniden.exchange.repository.HandoverRepository;
import com.yeniden.exchange.repository.HandoverOutboxEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yeniden.exchange.repository.ListingRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Exchange Service İş Mantığı Gerçekleşimi (ADR-005).
 */
@Service
@RequiredArgsConstructor
public class ExchangeServiceImpl implements ExchangeService {

    private final ListingRequestRepository requestRepository;
    private final HandoverRepository handoverRepository;
    private final HandoverOutboxEventRepository outboxRepository;
    private final ObjectMapper objectMapper;
    private final CatalogRewardContextClient catalogRewardContextClient;
    private final Random random = new Random();

    @Override
    @Transactional
    public ListingRequestDto createRequest(UUID listingId, CreateRequestDto requestDto) {
        ListingRequest request = ListingRequest.builder()
                .listingId(listingId)
                .requesterId(requestDto.getRequesterId())
                .ownerId(requestDto.getOwnerId())
                .message(requestDto.getMessage())
                .status(RequestStatus.PENDING)
                .build();

        ListingRequest saved = requestRepository.save(request);
        return mapToRequestDto(saved);
    }

    @Override
    @Transactional
    public HandoverDto acceptRequest(UUID requestId, UUID ownerId) {
        ListingRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new BaseException("İlan talebi bulunamadı!", "REQUEST_NOT_FOUND", 404));

        if (!request.getOwnerId().equals(ownerId)) {
            throw new BaseException("Bu talebi onaylama yetkiniz yok!", "FORBIDDEN", 403);
        }

        ListingRewardContext rewardContext = catalogRewardContextClient.get(request.getListingId());
        if (!rewardContext.ownerId().equals(ownerId)) {
            throw new BaseException("İlan sahibi bilgisi uyuşmuyor!", "LISTING_OWNER_MISMATCH", 409);
        }
        if (!"PUBLISHED".equals(rewardContext.listingStatus())) {
            throw new BaseException("Yayında olmayan ilan için teslimat başlatılamaz!", "LISTING_NOT_PUBLISHED", 409);
        }

        request.setStatus(RequestStatus.ACCEPTED);
        requestRepository.save(request);

        // ADR-005: 6 haneli tek kullanımlık rastgele doğrulama kodu üret ve SHA-256 ile hash'le
        String code = String.format("%06d", random.nextInt(1000000));
        String codeHash = sha256(code);

        Handover handover = Handover.builder()
                .requestId(request.getId())
                .listingId(request.getListingId())
                .providerId(request.getOwnerId())
                .receiverId(request.getRequesterId())
                .categoryId(rewardContext.categoryId())
                .categoryCoinMultiplier(rewardContext.categoryCoinMultiplier())
                .quantityBand(rewardContext.quantityBand())
                .reviewRequired(false)
                .confirmationCodeHash(codeHash)
                .rawCodeForReceiver(code)
                .status(HandoverStatus.PENDING_CODE)
                .failedAttempts(0)
                .expiresAt(LocalDateTime.now().plusHours(72))
                .build();

        Handover savedHandover = handoverRepository.save(handover);
        return mapToHandoverDto(savedHandover, null);
    }

    @Override
    @Transactional(readOnly = true)
    public HandoverDto getHandoverCode(UUID handoverId, UUID userId) {
        Handover handover = handoverRepository.findById(handoverId)
                .orElseThrow(() -> new BaseException("Teslimat kaydı bulunamadı!", "HANDOVER_NOT_FOUND", 404));

        // Kodu yalnızca teslim alacak olan kişi (receiverId) görüntüleyebilir
        if (!handover.getReceiverId().equals(userId)) {
            throw new BaseException("Teslimat kodunu görüntüleme yetkiniz yok!", "FORBIDDEN", 403);
        }

        return mapToHandoverDto(handover, handover.getRawCodeForReceiver());
    }

    @Override
    @Transactional
    public HandoverDto confirmHandoverCode(UUID handoverId, ConfirmCodeRequest request) {
        Handover handover = handoverRepository.findById(handoverId)
                .orElseThrow(() -> new BaseException("Teslimat kaydı bulunamadı!", "HANDOVER_NOT_FOUND", 404));

        if (!handover.getProviderId().equals(request.getProviderId())) {
            throw new BaseException("Teslimat kodunu onaylama yetkiniz yok!", "FORBIDDEN", 403);
        }

        if (handover.getStatus() != HandoverStatus.PENDING_CODE) {
            throw new BaseException("Bu teslimat zaten tamamlanmış veya süresi dolmuş!", "HANDOVER_NOT_PENDING", 409);
        }

        if (LocalDateTime.now().isAfter(handover.getExpiresAt())) {
            handover.setStatus(HandoverStatus.EXPIRED);
            handoverRepository.save(handover);
            throw new BaseException("Teslimat kodunun geçerlilik süresi dolmuş (72 saat)!", "CODE_EXPIRED", 400);
        }

        if (handover.getFailedAttempts() >= 5) {
            handover.setStatus(HandoverStatus.EXPIRED);
            handoverRepository.save(handover);
            throw new BaseException("5 hatalı deneme nedeniyle teslimat kodu iptal edildi!", "TOO_MANY_FAILED_ATTEMPTS", 400);
        }

        // Girilen kodun SHA-256 hash'i veritabanındaki hash ile eşleşiyor mu?
        String inputHash = sha256(request.getCode());
        if (!handover.getConfirmationCodeHash().equals(inputHash)) {
            handover.setFailedAttempts(handover.getFailedAttempts() + 1);
            handoverRepository.save(handover);
            throw new BaseException("Girilen teslimat kodu yanlış! Kalan deneme hakkı: " + (5 - handover.getFailedAttempts()), "INVALID_CODE", 400);
        }

        // Kod doğru -> Teslimat onaylandı!
        handover.setStatus(HandoverStatus.CONFIRMED);
        handover.setConfirmedAt(LocalDateTime.now());
        Handover updated = handoverRepository.save(handover);

        com.yeniden.common.event.HandoverConfirmedEvent event = com.yeniden.common.event.HandoverConfirmedEvent.builder()
                    .handoverId(updated.getId())
                    .requestId(updated.getRequestId())
                    .listingId(updated.getListingId())
                    .providerId(updated.getProviderId())
                    .receiverId(updated.getReceiverId())
                    .categoryId(updated.getCategoryId())
                    .categoryCoinMultiplier(updated.getCategoryCoinMultiplier())
                    .quantityBand(updated.getQuantityBand())
                    .reviewRequired(updated.isReviewRequired())
                    .handoverStatus(updated.getStatus().name())
                    .confirmedAt(updated.getConfirmedAt())
                    .build();
        writeOutbox(event);

        return mapToHandoverDto(updated, null);
    }

    private void writeOutbox(com.yeniden.common.event.HandoverConfirmedEvent event) {
        try {
            outboxRepository.save(HandoverOutboxEvent.builder()
                    .eventId(event.getHandoverId())
                    .eventType("HandoverConfirmedEvent")
                    .aggregateId(event.getHandoverId())
                    .payload(objectMapper.writeValueAsString(event))
                    .build());
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("HandoverConfirmedEvent serialize edilemedi", exception);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ListingRequestDto> getRequestsByListing(UUID listingId) {
        return requestRepository.findByListingId(listingId)
                .stream()
                .map(this::mapToRequestDto)
                .collect(Collectors.toList());
    }

    private String sha256(String data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algoritması bulunamadı", e);
        }
    }

    private ListingRequestDto mapToRequestDto(ListingRequest request) {
        return ListingRequestDto.builder()
                .id(request.getId())
                .listingId(request.getListingId())
                .requesterId(request.getRequesterId())
                .ownerId(request.getOwnerId())
                .message(request.getMessage())
                .status(request.getStatus())
                .createdAt(request.getCreatedAt())
                .build();
    }

    private HandoverDto mapToHandoverDto(Handover handover, String plainCode) {
        return HandoverDto.builder()
                .id(handover.getId())
                .requestId(handover.getRequestId())
                .listingId(handover.getListingId())
                .providerId(handover.getProviderId())
                .receiverId(handover.getReceiverId())
                .confirmationCode(plainCode)
                .status(handover.getStatus())
                .failedAttempts(handover.getFailedAttempts())
                .expiresAt(handover.getExpiresAt())
                .confirmedAt(handover.getConfirmedAt())
                .build();
    }
}
