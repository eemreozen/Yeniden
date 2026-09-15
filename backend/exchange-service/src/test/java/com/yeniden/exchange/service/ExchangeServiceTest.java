package com.yeniden.exchange.service;

import com.yeniden.common.event.HandoverConfirmedEvent;
import com.yeniden.common.exception.BaseException;
import com.yeniden.exchange.client.CatalogRewardContextClient;
import com.yeniden.exchange.client.ListingRewardContext;
import com.yeniden.exchange.config.RabbitMQConfig;
import com.yeniden.exchange.domain.Handover;
import com.yeniden.exchange.domain.HandoverStatus;
import com.yeniden.exchange.domain.HandoverOutboxEvent;
import com.yeniden.exchange.domain.ListingRequest;
import com.yeniden.exchange.domain.RequestStatus;
import com.yeniden.exchange.dto.ConfirmCodeRequest;
import com.yeniden.exchange.dto.CreateRequestDto;
import com.yeniden.exchange.dto.HandoverDto;
import com.yeniden.exchange.dto.ListingRequestDto;
import com.yeniden.exchange.repository.HandoverRepository;
import com.yeniden.exchange.repository.HandoverOutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yeniden.exchange.repository.ListingRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExchangeServiceTest {

    @Mock
    private ListingRequestRepository requestRepository;

    @Mock
    private HandoverRepository handoverRepository;

    @Mock
    private HandoverOutboxEventRepository outboxRepository;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private CatalogRewardContextClient catalogRewardContextClient;

    @InjectMocks
    private ExchangeServiceImpl exchangeService;

    private UUID listingId;
    private UUID ownerId;
    private UUID requesterId;
    private ListingRequest request;
    private Handover handover;

    @BeforeEach
    void setUp() {
        listingId = UUID.randomUUID();
        ownerId = UUID.randomUUID();
        requesterId = UUID.randomUUID();

        request = ListingRequest.builder()
                .id(UUID.randomUUID())
                .listingId(listingId)
                .ownerId(ownerId)
                .requesterId(requesterId)
                .status(RequestStatus.PENDING)
                .build();

        String rawCode = "123456";
        String codeHash = sha256(rawCode);

        handover = Handover.builder()
                .id(UUID.randomUUID())
                .requestId(request.getId())
                .listingId(listingId)
                .providerId(ownerId)
                .receiverId(requesterId)
                .categoryId(categoryId())
                .categoryCoinMultiplier(new BigDecimal("1.50"))
                .quantityBand("SINGLE")
                .confirmationCodeHash(codeHash)
                .status(HandoverStatus.PENDING_CODE)
                .failedAttempts(0)
                .expiresAt(LocalDateTime.now().plusHours(72))
                .build();
    }

    @Test
    @DisplayName("İlan talebi oluşturma senaryosu")
    void createRequest_Success() {
        CreateRequestDto createDto = new CreateRequestDto();
        createDto.setOwnerId(ownerId);
        createDto.setRequesterId(requesterId);
        createDto.setMessage("Ürünü alabilir miyim?");

        when(requestRepository.save(any(ListingRequest.class))).thenReturn(request);

        ListingRequestDto result = exchangeService.createRequest(listingId, createDto);

        assertNotNull(result);
        assertEquals(RequestStatus.PENDING, result.getStatus());
        verify(requestRepository).save(any(ListingRequest.class));
    }

    @Test
    @DisplayName("İlan talebini onaylama ve 6 haneli kodlu teslimat oluşturma")
    void acceptRequest_Success() {
        when(requestRepository.findById(request.getId())).thenReturn(Optional.of(request));
        when(catalogRewardContextClient.get(listingId)).thenReturn(rewardContext(ownerId));
        when(handoverRepository.save(any(Handover.class))).thenReturn(handover);

        HandoverDto result = exchangeService.acceptRequest(request.getId(), ownerId);

        assertNotNull(result);
        assertEquals(HandoverStatus.PENDING_CODE, result.getStatus());
        verify(handoverRepository).save(any(Handover.class));
    }

    @Test
    @DisplayName("İlan sahibi uyuşmazsa teslimat snapshot'ı oluşturulmaz")
    void acceptRequest_OwnerMismatch() {
        when(requestRepository.findById(request.getId())).thenReturn(Optional.of(request));
        when(catalogRewardContextClient.get(listingId)).thenReturn(rewardContext(UUID.randomUUID()));

        BaseException exception = assertThrows(BaseException.class,
                () -> exchangeService.acceptRequest(request.getId(), ownerId));

        assertEquals("LISTING_OWNER_MISMATCH", exception.getErrorCode());
    }

    @Test
    @DisplayName("Doğru kod girildiğinde teslimatın onaylanması ve RabbitMQ event yayınlanması")
    void confirmHandoverCode_Success() throws Exception {
        UUID handoverId = handover.getId();
        ConfirmCodeRequest confirmRequest = new ConfirmCodeRequest();
        confirmRequest.setProviderId(ownerId);
        confirmRequest.setCode("123456");

        Handover confirmedHandover = Handover.builder()
                .id(handoverId)
                .providerId(ownerId)
                .receiverId(requesterId)
                .listingId(listingId)
                .requestId(request.getId())
                .categoryId(handover.getCategoryId())
                .categoryCoinMultiplier(handover.getCategoryCoinMultiplier())
                .quantityBand(handover.getQuantityBand())
                .reviewRequired(false)
                .status(HandoverStatus.CONFIRMED)
                .confirmedAt(LocalDateTime.now())
                .build();

        when(handoverRepository.findById(handoverId)).thenReturn(Optional.of(handover));
        when(handoverRepository.save(any(Handover.class))).thenReturn(confirmedHandover);
        when(objectMapper.writeValueAsString(any(HandoverConfirmedEvent.class))).thenReturn("{\"event\":\"ok\"}");

        HandoverDto result = exchangeService.confirmHandoverCode(handoverId, confirmRequest);

        assertNotNull(result);
        assertEquals(HandoverStatus.CONFIRMED, result.getStatus());

        ArgumentCaptor<HandoverConfirmedEvent> eventCaptor = ArgumentCaptor.forClass(HandoverConfirmedEvent.class);
        verify(objectMapper).writeValueAsString(eventCaptor.capture());
        verify(outboxRepository).save(any(HandoverOutboxEvent.class));
        HandoverConfirmedEvent event = eventCaptor.getValue();
        assertEquals(handoverId, event.getHandoverId());
        assertEquals(handover.getCategoryId(), event.getCategoryId());
        assertEquals(new BigDecimal("1.50"), event.getCategoryCoinMultiplier());
        assertEquals("SINGLE", event.getQuantityBand());
        assertEquals("CONFIRMED", event.getHandoverStatus());
    }

    @Test
    @DisplayName("Hatalı kod girildiğinde kısıtlayıcı hata verilmesi")
    void confirmHandoverCode_WrongCode_ThrowsException() {
        UUID handoverId = handover.getId();
        ConfirmCodeRequest confirmRequest = new ConfirmCodeRequest();
        confirmRequest.setProviderId(ownerId);
        confirmRequest.setCode("999999"); // Yanlış kod

        when(handoverRepository.findById(handoverId)).thenReturn(Optional.of(handover));

        BaseException exception = assertThrows(BaseException.class,
                () -> exchangeService.confirmHandoverCode(handoverId, confirmRequest));

        assertEquals("INVALID_CODE", exception.getErrorCode());
        assertEquals(400, exception.getHttpStatus());
    }

    private String sha256(String data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private UUID categoryId() {
        return UUID.nameUUIDFromBytes("test-category".getBytes(StandardCharsets.UTF_8));
    }

    private ListingRewardContext rewardContext(UUID listingOwnerId) {
        return new ListingRewardContext(listingId, listingOwnerId, categoryId(),
                new BigDecimal("1.50"), "SINGLE", "PUBLISHED");
    }
}
