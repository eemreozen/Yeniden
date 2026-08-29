package com.yeniden.catalog.repository;

import com.yeniden.catalog.domain.Listing;
import com.yeniden.catalog.domain.ListingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Listing Entity'si için veritabanı erişim arayüzü.
 */
@Repository
public interface ListingRepository extends JpaRepository<Listing, UUID> {

    List<Listing> findByStatus(ListingStatus status);

    List<Listing> findByOwnerId(UUID ownerId);

    /**
     * Konum bazlı basit yakınlık filtresi (Enlem/Boylam kare alanı araması)
     */
    @Query("SELECT l FROM Listing l WHERE l.status = :status AND " +
           "l.approxLatitude BETWEEN :minLat AND :maxLat AND " +
           "l.approxLongitude BETWEEN :minLon AND :maxLon")
    List<Listing> findNearbyListings(
            @Param("status") ListingStatus status,
            @Param("minLat") double minLat,
            @Param("maxLat") double maxLat,
            @Param("minLon") double minLon,
            @Param("maxLon") double maxLon
    );
}
