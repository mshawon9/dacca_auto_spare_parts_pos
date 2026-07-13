package com.daccaauto.pos.dto.purchase;

import java.math.BigDecimal;

public record PurchaseOrderResponse(
    Long id,
    String invoiceId,
    BigDecimal total,
    int lineCount
) {
}
