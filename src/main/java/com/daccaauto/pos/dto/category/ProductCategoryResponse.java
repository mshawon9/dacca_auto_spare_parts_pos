package com.daccaauto.pos.dto.category;

public record ProductCategoryResponse(
    Long id,
    String name,
    String description,
    boolean active
) {
}