package com.daccaauto.pos.dto.sale;

import java.math.BigDecimal;

public record CustomerLedgerSummary(
    String customerName,
    BigDecimal totalSale,
    BigDecimal totalCollection,
    BigDecimal balanceDue
) {
}
