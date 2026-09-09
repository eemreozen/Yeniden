package com.yeniden.catalog.service;

import com.yeniden.catalog.domain.ItemCondition;
import com.yeniden.catalog.domain.ItemCategory;
import com.yeniden.catalog.domain.Listing;
import com.yeniden.catalog.domain.ListingStatus;
import com.yeniden.catalog.domain.QuantityBand;
import com.yeniden.catalog.config.RabbitMQConfig;
import com.yeniden.catalog.dto.ListingCreateRequest;
import com.yeniden.catalog.dto.ListingDto;
import com.yeniden.catalog.dto.ListingRewardContextDto;
import com.yeniden.catalog.repository.ItemCategoryRepository;
import com.yeniden.catalog.dto.ListingUpdateRequest;
import com.yeniden.catalog.repository.ListingRepository;
import com.yeniden.common.event.ListingPublishedEvent;
import com.yeniden.common.exception.BaseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CatalogServiceTest {

    @Mock
    private ListingRepository listingRepository;

    @Mock
    private ItemCategoryRepository itemCategoryRepository;

    @Mock
    private CategoryService categoryService;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private ListingServiceImpl listingService;

    private ListingCreateRequest createRequest;
    private Listing listing;
    private UUID categoryId;

    @BeforeEach
    void setUp() {
        categoryId = UUID.randomUUID();
        createRequest = new ListingCreateRequest();
        createRequest.setOwnerId(UUID.randomUUID());
        createRequest.setCategoryId(categoryId);
        createRequest.setTitle("Temiz Ahşap Masa");
        createRequest.setDescription("Az kullanılmış, sağlam 4 kişilik ahşap masa.");
        createRequest.setCondition(ItemCondition.NEW_LIKE);
        createRequest.setQuantityBand(QuantityBand.SINGLE);
        createRequest.setLatitude(41.0082);
        createRequest.setLongitude(28.9784);

        listing = Listing.builder()
                .id(UUID.randomUUID())
                .ownerId(createRequest.getOwnerId())
                .categoryId(categoryId)
                .title(createRequest.getTitle())
                .description(createRequest.getDescription())
                .condition(ItemCondition.NEW_LIKE)
                .quantityBand(QuantityBand.SINGLE)
                .status(ListingStatus.PUBLISHED)
                .approxLatitude(41.0082)
                .approxLongitude(28.9784)
                .build();
    }

    @Test
    @DisplayName("Başarılı ilan oluşturma senaryosu")
    void createListing_Success() {
        when(listingRepository.save(any(Listing.class))).thenReturn(listing);

        ListingDto result = listingService.createListing(createRequest);

        assertNotNull(result);
        assertEquals("Temiz Ahşap Masa", result.getTitle());
        assertEquals(ListingStatus.PUBLISHED, result.getStatus());

        verify(listingRepository).save(any(Listing.class));
    }

    @Test
    @DisplayName("İlan yayınlandığında ListingPublished event'i yayınlanır")
    void publishListing_Success_PublishesEvent() {
        UUID listingId = listing.getId();
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));
        when(listingRepository.save(any(Listing.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ListingDto result = listingService.publishListing(listingId);

        assertEquals(ListingStatus.PUBLISHED, result.getStatus());
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.EXCHANGE),
                eq(RabbitMQConfig.LISTING_PUBLISHED_ROUTING_KEY),
                any(ListingPublishedEvent.class));
    }

    @Test
    @DisplayName("ID ile ilan getirme senaryosu")
    void getListingById_Success() {
        UUID listingId = listing.getId();
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));

        ListingDto result = listingService.getListingById(listingId);

        assertNotNull(result);
        assertEquals(listingId, result.getId());
    }

    @Test
    @DisplayName("Bulunamayan ilan ID'si ile sorgulamada hatanın fırlatılması")
    void getListingById_NotFound_ThrowsException() {
        UUID listingId = UUID.randomUUID();
        when(listingRepository.findById(listingId)).thenReturn(Optional.empty());

        BaseException exception = assertThrows(BaseException.class, () -> listingService.getListingById(listingId));

        assertEquals("LISTING_NOT_FOUND", exception.getErrorCode());
        assertEquals(404, exception.getHttpStatus());
    }

    @Test
    @DisplayName("İlan ödül bağlamı kategori katsayısıyla birlikte döner")
    void getRewardContext_Success() {
        ItemCategory category = ItemCategory.builder()
                .id(categoryId)
                .code("WOOD")
                .name("Ahşap")
                .coinMultiplier(1.5)
                .build();
        when(listingRepository.findById(listing.getId())).thenReturn(Optional.of(listing));
        when(itemCategoryRepository.findById(categoryId)).thenReturn(Optional.of(category));

        ListingRewardContextDto result = listingService.getRewardContext(listing.getId());

        assertEquals(listing.getId(), result.listingId());
        assertEquals(listing.getOwnerId(), result.ownerId());
        assertEquals(categoryId, result.categoryId());
        assertEquals("1.5", result.categoryCoinMultiplier().toPlainString());
        assertEquals("SINGLE", result.quantityBand());
        assertEquals("PUBLISHED", result.listingStatus());
    }

    @Test
    @DisplayName("Kategori bulunamazsa ödül bağlamı varsayılan katsayı üretmez")
    void getRewardContext_CategoryNotFound() {
        when(listingRepository.findById(listing.getId())).thenReturn(Optional.of(listing));
        when(itemCategoryRepository.findById(categoryId)).thenReturn(Optional.empty());

        BaseException exception = assertThrows(BaseException.class,
                () -> listingService.getRewardContext(listing.getId()));

        assertEquals("CATEGORY_NOT_FOUND", exception.getErrorCode());
        assertEquals(409, exception.getHttpStatus());
    }
    @Test
    @DisplayName("Sahibi yayındaki ilanı geri çekebilir")
    void withdrawListing_Success() {
        Listing published = Listing.builder()
                .id(listing.getId())
                .ownerId(listing.getOwnerId())
                .status(ListingStatus.PUBLISHED)
                .build();

        when(listingRepository.findById(published.getId())).thenReturn(Optional.of(published));
        when(listingRepository.save(any(Listing.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ListingDto result = listingService.withdrawListing(published.getId(), published.getOwnerId());

        assertEquals(ListingStatus.WITHDRAWN, result.getStatus());
    }

    @Test
    @DisplayName("Başka birinin ilanını geri çekme denemesi FORBIDDEN ile reddedilir")
    void withdrawListing_NotOwner_ThrowsForbidden() {
        Listing published = Listing.builder()
                .id(listing.getId())
                .ownerId(listing.getOwnerId())
                .status(ListingStatus.PUBLISHED)
                .build();

        when(listingRepository.findById(published.getId())).thenReturn(Optional.of(published));

        BaseException exception = assertThrows(BaseException.class,
                () -> listingService.withdrawListing(published.getId(), UUID.randomUUID()));

        assertEquals("FORBIDDEN", exception.getErrorCode());
        assertEquals(403, exception.getHttpStatus());
    }

    @Test
    @DisplayName("Yayında olmayan ilan geri çekilemez")
    void withdrawListing_NotPublished_ThrowsConflict() {
        Listing draft = Listing.builder()
                .id(listing.getId())
                .ownerId(listing.getOwnerId())
                .status(ListingStatus.DRAFT)
                .build();

        when(listingRepository.findById(draft.getId())).thenReturn(Optional.of(draft));

        BaseException exception = assertThrows(BaseException.class,
                () -> listingService.withdrawListing(draft.getId(), draft.getOwnerId()));

        assertEquals("LISTING_NOT_PUBLISHED", exception.getErrorCode());
        assertEquals(409, exception.getHttpStatus());
    }

    @Test
    @DisplayName("Sahibi taslak ilanın başlığını güncelleyebilir")
    void updateListing_Success() {
        Listing draft = Listing.builder()
                .id(listing.getId())
                .ownerId(listing.getOwnerId())
                .title("Eski başlık")
                .status(ListingStatus.DRAFT)
                .build();

        ListingUpdateRequest request = ListingUpdateRequest.builder().title("Yeni başlık").build();

        when(listingRepository.findById(draft.getId())).thenReturn(Optional.of(draft));
        when(listingRepository.save(any(Listing.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ListingDto result = listingService.updateListing(draft.getId(), draft.getOwnerId(), request);

        assertEquals("Yeni başlık", result.getTitle());
    }

    @Test
    @DisplayName("Kapanmış ilan güncellenemez")
    void updateListing_NotEditableStatus_ThrowsConflict() {
        Listing closed = Listing.builder()
                .id(listing.getId())
                .ownerId(listing.getOwnerId())
                .status(ListingStatus.CLOSED)
                .build();

        when(listingRepository.findById(closed.getId())).thenReturn(Optional.of(closed));

        BaseException exception = assertThrows(BaseException.class,
                () -> listingService.updateListing(closed.getId(), closed.getOwnerId(),
                        ListingUpdateRequest.builder().title("x").build()));

        assertEquals("LISTING_NOT_EDITABLE", exception.getErrorCode());
        assertEquals(409, exception.getHttpStatus());
    }

    @Test
    @DisplayName("Sahibin ilanları durum filtresiyle listelenir")
    void getListingsByOwner_WithStatusFilter_ReturnsFilteredList() {
        UUID ownerId = listing.getOwnerId();
        when(listingRepository.findByOwnerIdAndStatus(ownerId, ListingStatus.PUBLISHED))
                .thenReturn(java.util.List.of(listing));

        var result = listingService.getListingsByOwner(ownerId, ListingStatus.PUBLISHED);

        assertEquals(1, result.size());
        verify(listingRepository).findByOwnerIdAndStatus(ownerId, ListingStatus.PUBLISHED);
        verify(listingRepository, never()).findByOwnerId(any());
    }

    @Test
    @DisplayName("Yakınlık araması kategori alt ağacını çözer ve mesafe alanını doldurur")
    void searchNearby_WithCategoryFilter_ResolvesSubtreeAndReturnsDistance() {
        UUID parentCategoryId = UUID.randomUUID();
        List<UUID> subtreeIds = List.of(parentCategoryId, categoryId);

        Listing nearby = Listing.builder()
                .id(UUID.randomUUID())
                .ownerId(listing.getOwnerId())
                .status(ListingStatus.PUBLISHED)
                .approxLatitude(41.0090)
                .approxLongitude(28.9790)
                .build();

        when(categoryService.getSubtreeIds(parentCategoryId)).thenReturn(subtreeIds);
        when(listingRepository.searchNearby(41.0082, 28.9784, 3000.0, subtreeIds, null, null, "distance", 20))
                .thenReturn(List.of(nearby));

        List<ListingDto> result = listingService.searchNearby(
                41.0082, 28.9784, 3000.0, parentCategoryId, null, null, "distance", 20);

        assertEquals(1, result.size());
        assertNotNull(result.get(0).getDistanceMeters());
        verify(categoryService).getSubtreeIds(parentCategoryId);
    }

    @Test
    @DisplayName("Kategori filtresi verilmediğinde alt ağaç çözümü yapılmaz")
    void searchNearby_WithoutCategoryFilter_DoesNotResolveSubtree() {
        when(listingRepository.searchNearby(41.0082, 28.9784, 3000.0, null, null, null, "distance", 20))
                .thenReturn(List.of());

        listingService.searchNearby(41.0082, 28.9784, 3000.0, null, null, null, "distance", 20);

        verify(categoryService, never()).getSubtreeIds(any());
    }

    @Test
    @DisplayName("Yayındaki ilan başarıyla rezerve edilir")
    void reserveListing_Success() {
        UUID requestId = UUID.randomUUID();
        when(listingRepository.reserveIfCurrentStatus(listing.getId(), requestId, ListingStatus.RESERVED, ListingStatus.PUBLISHED))
                .thenReturn(1);
        when(listingRepository.findById(listing.getId())).thenReturn(Optional.of(listing));

        ListingDto result = listingService.reserveListing(listing.getId(), requestId);

        assertNotNull(result);
    }

    @Test
    @DisplayName("Artık müsait olmayan ilan rezerve edilemez")
    void reserveListing_NotAvailable_ThrowsConflict() {
        UUID requestId = UUID.randomUUID();
        when(listingRepository.reserveIfCurrentStatus(listing.getId(), requestId, ListingStatus.RESERVED, ListingStatus.PUBLISHED))
                .thenReturn(0);

        BaseException exception = assertThrows(BaseException.class,
                () -> listingService.reserveListing(listing.getId(), requestId));

        assertEquals("LISTING_NOT_AVAILABLE", exception.getErrorCode());
        assertEquals(409, exception.getHttpStatus());
    }

    @Test
    @DisplayName("Rezerve edilmiş ilan serbest bırakılınca tekrar PUBLISHED olur")
    void releaseReservation_Success() {
        Listing reserved = Listing.builder()
                .id(listing.getId())
                .ownerId(listing.getOwnerId())
                .status(ListingStatus.RESERVED)
                .reservedRequestId(UUID.randomUUID())
                .build();

        when(listingRepository.findById(reserved.getId())).thenReturn(Optional.of(reserved));
        when(listingRepository.save(any(Listing.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ListingDto result = listingService.releaseReservation(reserved.getId());

        assertEquals(ListingStatus.PUBLISHED, result.getStatus());
    }
}
