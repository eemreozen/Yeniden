package com.yeniden.catalog.repository;

import com.yeniden.catalog.domain.Listing;
import com.yeniden.catalog.domain.QuantityBand;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * catalog.listings üzerinde gerçek PostGIS ST_DWithin/ST_Distance sorgusu.
 * Depoda ayrı bir geography sütunu yerine mevcut approx_latitude/approx_longitude
 * çiftinden anlık nokta üretilir; bu, mevcut veriye dokunmadan gerçek coğrafi
 * mesafe hesabı (great-circle) sağlar. Filtreler yalnızca gerçekten verildiğinde
 * SQL'e eklenir; boş/null koleksiyon veya metin native IN/ILIKE'a bağlanmaz.
 */
@Repository
public class ListingSearchRepositoryImpl implements ListingSearchRepository {

    private final EntityManager entityManager;

    public ListingSearchRepositoryImpl(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public List<Listing> searchNearby(double lat, double lon, double radiusMeters,
                                       List<UUID> categoryIds, QuantityBand quantityBand,
                                       String searchText, String sort, int limit) {
        boolean hasCategoryFilter = categoryIds != null && !categoryIds.isEmpty();
        boolean hasQuantityFilter = quantityBand != null;
        boolean hasSearchText = searchText != null && !searchText.isBlank();

        StringBuilder sql = new StringBuilder(
                "SELECT * FROM catalog.listings l WHERE l.status = 'PUBLISHED' " +
                "AND ST_DWithin(" +
                "ST_SetSRID(ST_MakePoint(l.approx_longitude, l.approx_latitude), 4326)::geography, " +
                "ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)::geography, " +
                ":radiusMeters)");

        if (hasCategoryFilter) {
            sql.append(" AND l.category_id IN (:categoryIds)");
        }
        if (hasQuantityFilter) {
            sql.append(" AND l.quantity_band = :quantityBand");
        }
        if (hasSearchText) {
            sql.append(" AND (l.title ILIKE :searchText OR l.description ILIKE :searchText)");
        }

        if ("newest".equalsIgnoreCase(sort)) {
            sql.append(" ORDER BY l.published_at DESC");
        } else {
            sql.append(" ORDER BY ST_Distance(" +
                    "ST_SetSRID(ST_MakePoint(l.approx_longitude, l.approx_latitude), 4326)::geography, " +
                    "ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)::geography) ASC");
        }
        sql.append(" LIMIT :limit");

        Query nativeQuery = entityManager.createNativeQuery(sql.toString(), Listing.class);
        nativeQuery.setParameter("lat", lat);
        nativeQuery.setParameter("lon", lon);
        nativeQuery.setParameter("radiusMeters", radiusMeters);
        nativeQuery.setParameter("limit", limit);
        if (hasCategoryFilter) {
            nativeQuery.setParameter("categoryIds", categoryIds);
        }
        if (hasQuantityFilter) {
            nativeQuery.setParameter("quantityBand", quantityBand.name());
        }
        if (hasSearchText) {
            nativeQuery.setParameter("searchText", "%" + searchText + "%");
        }

        @SuppressWarnings("unchecked")
        List<Listing> results = nativeQuery.getResultList();
        return results;
    }
}
