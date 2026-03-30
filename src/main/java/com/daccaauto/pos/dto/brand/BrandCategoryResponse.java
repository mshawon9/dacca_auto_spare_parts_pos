package com.daccaauto.pos.dto.brand;

public record BrandCategoryResponse(
    Long id,
    Long brandId,
    String brandName,
    Long categoryId,
    String categoryName,
    Integer displayOrder,
    boolean active
) {
}