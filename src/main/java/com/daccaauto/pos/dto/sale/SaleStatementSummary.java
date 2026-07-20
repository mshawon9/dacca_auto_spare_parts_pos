package com.daccaauto.pos.dto.sale;

import java.math.BigDecimal;

public record SaleStatementSummary(
    long invoiceCount,
    BigDecimal subTotal,
    BigDecimal vatAmount,
    BigDecimal total,
    BigDecimal paidAmount,
    BigDecimal balanceDue
) {
}
