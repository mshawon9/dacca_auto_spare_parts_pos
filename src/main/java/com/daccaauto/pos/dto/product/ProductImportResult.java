package com.daccaauto.pos.dto.product;

import java.util.List;

public record ProductImportResult(
    int totalRows,
    int successCount,
    int failedCount,
    List<RowResult> rows
) {

    public record RowResult(
        int rowNumber,
        String category,
        String brand,
        String name,
        String partNumber,
        boolean success,
        String message
    ) {
    }
}
