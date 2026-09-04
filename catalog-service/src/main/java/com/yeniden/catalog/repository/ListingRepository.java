package com.yeniden.catalog.repository;

import com.yeniden.catalog.domain.Listing;
import com.yeniden.catalog.domain.ListingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Listing Entity'si için veritabanı erişim arayüzü.
 */
@Repository
public interface ListingRepository extends JpaRepository<Listing, UUID>, ListingSearchRepository {

    List<Listing> findByStatus(ListingStatus status);

    List<Listing> findByOwnerId(UUID ownerId);

    List<Listing> findByOwnerIdAndStatus(UUID ownerId, ListingStatus status);

    /**
     * İlanı yalnızca hâlâ {@code currentStatus} durumundaysa {@code newStatus}'a taşır ve
     * rezervasyon talebini işler (03-flows.md: "UPDATE ... WHERE status='PUBLISHED'").
     * Dönen satır sayısı 0 ise ilan artık müsait değildir (yarış koşulu koruması).
     */
    @Modifying
    @Query("UPDATE Listing l SET l.status = :newStatus, l.reservedRequestId = :requestId " +
           "WHERE l.id = :id AND l.status = :currentStatus")
    int reserveIfCurrentStatus(@Param("id") UUID id, @Param("requestId") UUID requestId,
                                @Param("newStatus") ListingStatus newStatus,
                                @Param("currentStatus") ListingStatus currentStatus);
}
