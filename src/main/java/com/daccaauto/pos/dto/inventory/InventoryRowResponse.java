package com.daccaauto.pos.dto.inventory;

import java.math.BigDecimal;

public record InventoryRowResponse(
    Long productId,
    String productName,
    String partNumber,
    String sku,
    String categoryName,
    String brandName,
    boolean hasImage,
    BigDecimal quantity,
    BigDecimal sellingPrice,
    BigDecimal previousPrice,
    BigDecimal costPrice,
    BigDecimal previousCostPrice
) {
}
