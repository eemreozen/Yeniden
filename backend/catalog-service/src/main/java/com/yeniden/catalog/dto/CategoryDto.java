package com.yeniden.catalog.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

/**
 * Kategori ağacı düğümünü sunan DTO.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryDto {
    private UUID id;
    private String code;
    private String name;
    private boolean reusable;
    private double coinMultiplier;
    private int sortOrder;
    private List<CategoryDto> children;
}
