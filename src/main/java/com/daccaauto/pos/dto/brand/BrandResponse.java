package com.daccaauto.pos.dto.brand;

public record BrandResponse(
    Long id,
    String name,
    String code,
    boolean active
) {
}