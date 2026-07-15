package com.daccaauto.pos.dto.sale;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record SaleDraftSummary(
    Long id,
    String customerName,
    String storeName,
    int lineCount,
    BigDecimal total,
    OffsetDateTime updatedAt
) {
}
