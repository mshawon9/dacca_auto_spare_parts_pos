package com.daccaauto.pos.dto.supplier;

import java.math.BigDecimal;

public record ProductSupplierResponse(
    Long id,
    Long productId,
    String productName,
    String productPartNumber,
    Long supplierId,
    String supplierName,
    String supplierTrnNumber,
    String supplierProductCode,
    String priceInput,
    BigDecimal priceValue,
    String currency,
    String notes,
    boolean active
) {
}