package com.yeniden.catalog.controller;

import com.yeniden.common.result.ApiResponse;
import com.yeniden.catalog.dto.CategoryDto;
import com.yeniden.catalog.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Kategori Ağacı REST API Uç Noktaları.
 */
@RestController
@RequestMapping("/api/v1/catalog")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<CategoryDto>>> getCategoryTree() {
        return ResponseEntity.ok(ApiResponse.success(categoryService.getCategoryTree()));
    }
}
