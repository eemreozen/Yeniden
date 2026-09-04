package com.yeniden.catalog.service;

import com.yeniden.catalog.domain.ItemCategory;
import com.yeniden.catalog.dto.CategoryDto;
import com.yeniden.catalog.repository.ItemCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Kategori ağacı iş mantığı gerçeklemesi.
 */
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final ItemCategoryRepository categoryRepository;

    @Override
    @Transactional(readOnly = true)
    public List<CategoryDto> getCategoryTree() {
        List<ItemCategory> all = categoryRepository.findAll();
        Map<UUID, List<ItemCategory>> childrenByParent = groupByParent(all);

        return all.stream()
                .filter(category -> category.getParentId() == null)
                .sorted(Comparator.comparingInt(ItemCategory::getSortOrder))
                .map(root -> mapToDto(root, childrenByParent))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<UUID> getSubtreeIds(UUID categoryId) {
        Map<UUID, List<ItemCategory>> childrenByParent = groupByParent(categoryRepository.findAll());

        List<UUID> ids = new ArrayList<>();
        collectSubtreeIds(categoryId, childrenByParent, ids);
        return ids;
    }

    private void collectSubtreeIds(UUID categoryId, Map<UUID, List<ItemCategory>> childrenByParent, List<UUID> collected) {
        collected.add(categoryId);
        for (ItemCategory child : childrenByParent.getOrDefault(categoryId, List.of())) {
            collectSubtreeIds(child.getId(), childrenByParent, collected);
        }
    }

    private Map<UUID, List<ItemCategory>> groupByParent(List<ItemCategory> all) {
        return all.stream()
                .filter(category -> category.getParentId() != null)
                .collect(Collectors.groupingBy(ItemCategory::getParentId));
    }

    private CategoryDto mapToDto(ItemCategory category, Map<UUID, List<ItemCategory>> childrenByParent) {
        List<CategoryDto> children = childrenByParent.getOrDefault(category.getId(), List.of())
                .stream()
                .sorted(Comparator.comparingInt(ItemCategory::getSortOrder))
                .map(child -> mapToDto(child, childrenByParent))
                .collect(Collectors.toList());

        return CategoryDto.builder()
                .id(category.getId())
                .code(category.getCode())
                .name(category.getName())
                .reusable(category.isReusable())
                .coinMultiplier(category.getCoinMultiplier())
                .sortOrder(category.getSortOrder())
                .children(children)
                .build();
    }
}
