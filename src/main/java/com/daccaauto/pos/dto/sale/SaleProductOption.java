package com.daccaauto.pos.dto.sale;

import java.math.BigDecimal;

public record SaleProductOption(
    Long id,
    String text,
    BigDecimal stockQuantity,
    BigDecimal lastCostPrice,
    BigDecimal sellingPrice,
    BigDecimal customerPrice
) {
}
