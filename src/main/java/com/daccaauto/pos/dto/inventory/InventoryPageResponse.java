package com.daccaauto.pos.dto.inventory;

import java.util.List;

public record InventoryPageResponse(
    List<InventoryRowResponse> rows,
    int pageNumber,
    int pageSize,
    int totalPages,
    long totalElements,
    boolean first,
    boolean last
) {
    public static InventoryPageResponse empty(int pageSize) {
        return new InventoryPageResponse(List.of(), 0, pageSize, 0, 0, true, true);
    }
}
