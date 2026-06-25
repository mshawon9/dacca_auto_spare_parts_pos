package com.daccaauto.pos.dto.inventory;

public record StoreResponse(
    Long id,
    String name,
    String code,
    String address
) {
}
