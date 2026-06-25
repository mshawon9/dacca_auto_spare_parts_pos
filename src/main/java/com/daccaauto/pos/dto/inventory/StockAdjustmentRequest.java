package com.daccaauto.pos.dto.inventory;

import com.daccaauto.pos.entity.StockAdjustmentType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record StockAdjustmentRequest(
    @NotNull Long storeId,
    @NotNull Long productId,
    @NotNull StockAdjustmentType adjustmentType,
    @NotNull @DecimalMin("0.000") @Digits(integer = 16, fraction = 3) BigDecimal quantity,
    @Size(max = 250) String note
) {
}
