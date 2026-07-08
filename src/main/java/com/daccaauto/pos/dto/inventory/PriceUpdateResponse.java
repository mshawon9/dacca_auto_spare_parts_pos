package com.daccaauto.pos.dto.inventory;

import java.math.BigDecimal;

public record PriceUpdateResponse(
    Long storeId,
    Long productId,
    BigDecimal oldPrice,
    BigDecimal newPrice,
    BigDecimal oldCostPrice,
    BigDecimal newCostPrice
) {
}
