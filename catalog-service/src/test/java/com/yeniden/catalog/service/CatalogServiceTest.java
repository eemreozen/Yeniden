package com.yeniden.catalog.service;

import com.yeniden.catalog.domain.ItemCondition;
import com.yeniden.catalog.domain.Listing;
import com.yeniden.catalog.domain.ListingStatus;
import com.yeniden.catalog.domain.QuantityBand;
import com.yeniden.catalog.dto.ListingCreateRequest;
import com.yeniden.catalog.dto.ListingDto;
import com.yeniden.catalog.repository.ListingRepository;
import com.yeniden.common.exception.BaseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CatalogServiceTest {

    @Mock
    private ListingRepository listingRepository;

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
}
