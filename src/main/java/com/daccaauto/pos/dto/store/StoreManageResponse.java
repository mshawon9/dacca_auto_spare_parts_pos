package com.daccaauto.pos.dto.store;

import java.time.OffsetDateTime;

public record StoreManageResponse(
    Long id,
    String name,
    String code,
    String address,
    boolean active,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {
}
