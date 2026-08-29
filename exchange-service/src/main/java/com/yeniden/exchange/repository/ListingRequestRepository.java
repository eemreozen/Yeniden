package com.yeniden.exchange.repository;

import com.yeniden.exchange.domain.ListingRequest;
import com.yeniden.exchange.domain.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * ListingRequest Entity'si için veritabanı erişim arayüzü.
 */
@Repository
public interface ListingRequestRepository extends JpaRepository<ListingRequest, UUID> {
    List<ListingRequest> findByListingId(UUID listingId);
    List<ListingRequest> findByRequesterId(UUID requesterId);
    Optional<ListingRequest> findByListingIdAndRequesterIdAndStatus(UUID listingId, UUID requesterId, RequestStatus status);
}
