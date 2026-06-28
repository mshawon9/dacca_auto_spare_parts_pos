package com.daccaauto.pos.dto.inventory;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record PriceHistoryResponse(
    BigDecimal oldPrice,
    BigDecimal newPrice,
    String note,
    OffsetDateTime changedAt
) {
}
