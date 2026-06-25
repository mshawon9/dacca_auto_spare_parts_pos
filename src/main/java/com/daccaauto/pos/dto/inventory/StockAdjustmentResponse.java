package com.daccaauto.pos.dto.inventory;

import com.daccaauto.pos.entity.StockAdjustmentType;

import java.math.BigDecimal;

public record StockAdjustmentResponse(
    Long productId,
    Long storeId,
    StockAdjustmentType adjustmentType,
    BigDecimal previousQuantity,
    BigDecimal adjustmentQuantity,
    BigDecimal newQuantity
) {
}
