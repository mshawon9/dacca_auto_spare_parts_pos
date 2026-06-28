package com.daccaauto.pos.dto.inventory;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record PriceUpdateRequest(
    @NotNull Long storeId,
    @NotNull Long productId,
    @NotNull @DecimalMin("0.01") @Digits(integer = 17, fraction = 2) BigDecimal price,
    @Size(max = 250) String note
) {
}
