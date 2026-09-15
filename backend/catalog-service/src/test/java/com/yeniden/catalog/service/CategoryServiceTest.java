package com.yeniden.catalog.service;

import com.yeniden.catalog.domain.ItemCategory;
import com.yeniden.catalog.dto.CategoryDto;
import com.yeniden.catalog.repository.ItemCategoryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private ItemCategoryRepository categoryRepository;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    @Test
    @DisplayName("Alt kategoriler doğru üst kategori altında ağaç olarak döner")
    void getCategoryTree_NestsChildrenUnderParent() {
        UUID glassId = UUID.randomUUID();

        ItemCategory glass = ItemCategory.builder()
                .id(glassId)
                .code("GLASS")
                .name("Cam")
                .sortOrder(1)
                .reusable(true)
                .coinMultiplier(1.2)
                .build();

        ItemCategory jar = ItemCategory.builder()
                .id(UUID.randomUUID())
                .parentId(glassId)
                .code("GLASS_JAR")
                .name("Kavanoz")
                .sortOrder(1)
                .reusable(true)
                .coinMultiplier(1.2)
                .build();

        ItemCategory bottle = ItemCategory.builder()
                .id(UUID.randomUUID())
                .parentId(glassId)
                .code("GLASS_BOTTLE")
                .name("Şişe")
                .sortOrder(2)
                .reusable(true)
                .coinMultiplier(1.2)
                .build();

        when(categoryRepository.findAll()).thenReturn(List.of(bottle, glass, jar));

        List<CategoryDto> tree = categoryService.getCategoryTree();

        assertEquals(1, tree.size());
        CategoryDto glassDto = tree.get(0);
        assertEquals("GLASS", glassDto.getCode());
        assertEquals(2, glassDto.getChildren().size());
        assertEquals("GLASS_JAR", glassDto.getChildren().get(0).getCode());
        assertEquals("GLASS_BOTTLE", glassDto.getChildren().get(1).getCode());
    }

    @Test
    @DisplayName("Alt kategorisi olmayan kök kategori boş children listesiyle döner")
    void getCategoryTree_LeafRoot_HasEmptyChildren() {
        ItemCategory carton = ItemCategory.builder()
                .id(UUID.randomUUID())
                .code("CARTON")
                .name("Karton")
                .sortOrder(1)
                .build();

        when(categoryRepository.findAll()).thenReturn(List.of(carton));

        List<CategoryDto> tree = categoryService.getCategoryTree();

        assertEquals(1, tree.size());
        assertTrue(tree.get(0).getChildren().isEmpty());
    }

    @Test
    @DisplayName("getSubtreeIds verilen kategori ile tüm torunlarını döner")
    void getSubtreeIds_ReturnsSelfAndDescendants() {
        UUID glassId = UUID.randomUUID();
        UUID jarId = UUID.randomUUID();
        UUID bottleId = UUID.randomUUID();

        ItemCategory glass = ItemCategory.builder().id(glassId).code("GLASS").name("Cam").build();
        ItemCategory jar = ItemCategory.builder().id(jarId).parentId(glassId).code("GLASS_JAR").name("Kavanoz").build();
        ItemCategory bottle = ItemCategory.builder().id(bottleId).parentId(glassId).code("GLASS_BOTTLE").name("Şişe").build();

        when(categoryRepository.findAll()).thenReturn(List.of(glass, jar, bottle));

        List<UUID> subtreeIds = categoryService.getSubtreeIds(glassId);

        assertEquals(3, subtreeIds.size());
        assertTrue(subtreeIds.containsAll(List.of(glassId, jarId, bottleId)));
    }

    @Test
    @DisplayName("getSubtreeIds yaprak kategori için yalnızca kendi id'sini döner")
    void getSubtreeIds_LeafCategory_ReturnsOnlyItself() {
        UUID cartonId = UUID.randomUUID();
        ItemCategory carton = ItemCategory.builder().id(cartonId).code("CARTON").name("Karton").build();

        when(categoryRepository.findAll()).thenReturn(List.of(carton));

        List<UUID> subtreeIds = categoryService.getSubtreeIds(cartonId);

        assertEquals(List.of(cartonId), subtreeIds);
    }
}
