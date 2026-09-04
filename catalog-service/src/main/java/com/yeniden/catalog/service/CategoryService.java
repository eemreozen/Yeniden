package com.yeniden.catalog.service;

import com.yeniden.catalog.dto.CategoryDto;

import java.util.List;
import java.util.UUID;

/**
 * Kategori ağacı iş mantığı arayüzü.
 */
public interface CategoryService {
    List<CategoryDto> getCategoryTree();

    /**
     * Verilen kategori ve tüm alt kategorilerinin id'lerini döner (categoryId filtresinin
     * alt kategorileri de kapsaması için, bkz. 05-api.md).
     */
    List<UUID> getSubtreeIds(UUID categoryId);
}
