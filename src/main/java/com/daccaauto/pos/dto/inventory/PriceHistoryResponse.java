package com.daccaauto.pos.dto.inventory;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record PriceHistoryResponse(
    BigDecimal oldPrice,
    BigDecimal newPrice,
    BigDecimal oldCostPrice,
    BigDecimal newCostPrice,
    String note,
    OffsetDateTime changedAt
) {
}
