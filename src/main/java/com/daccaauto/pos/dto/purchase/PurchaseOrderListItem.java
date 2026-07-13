package com.daccaauto.pos.dto.purchase;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PurchaseOrderListItem(
    Long id,
    String invoiceId,
    LocalDate purchaseDate,
    String supplierName,
    String storeName,
    BigDecimal total,
    int lineCount,
    BigDecimal returnedQuantity,
    boolean hasReturns
) {
}
