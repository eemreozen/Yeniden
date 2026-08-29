package com.yeniden.catalog.repository;

import com.yeniden.catalog.domain.ItemCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * ItemCategory Entity'si için veritabanı erişim arayüzü.
 */
@Repository
public interface ItemCategoryRepository extends JpaRepository<ItemCategory, UUID> {
    Optional<ItemCategory> findByCode(String code);
    List<ItemCategory> findByParentIdIsNullOrderBySortOrderAsc();
}
