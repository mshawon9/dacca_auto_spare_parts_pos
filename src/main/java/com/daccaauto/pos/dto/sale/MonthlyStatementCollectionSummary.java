package com.daccaauto.pos.dto.sale;

import java.math.BigDecimal;
import java.time.YearMonth;

public record MonthlyStatementCollectionSummary(
    Long customerId,
    String customerName,
    YearMonth statementMonth,
    long invoiceCount,
    BigDecimal total,
    BigDecimal paidAmount,
    BigDecimal balanceDue
) {
}
