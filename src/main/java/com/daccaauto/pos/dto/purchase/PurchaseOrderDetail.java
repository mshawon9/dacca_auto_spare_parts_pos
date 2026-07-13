package com.daccaauto.pos.dto.purchase;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record PurchaseOrderDetail(
    Long id,
    String invoiceId,
    LocalDate purchaseDate,
    Long supplierId,
    String supplierName,
    Long storeId,
    String storeName,
    BigDecimal total,
    boolean hasReturns,
    List<Line> lines
) {
    public record Line(
        Long id,
        Long productId,
        String productName,
        String partNumber,
        String brandName,
        String supplierProductCode,
        BigDecimal quantity,
        BigDecimal returnedQuantity,
        BigDecimal returnableQuantity,
        BigDecimal unitPrice,
        BigDecimal taxPercent,
        BigDecimal taxAmount,
        BigDecimal lineTotal
    ) {
    }
}
