package com.daccaauto.pos.dto.sale;

import java.math.BigDecimal;

public record CreditCollectionSummary(
    long invoiceCount,
    BigDecimal totalDue,
    BigDecimal overdueDue
) {
}
