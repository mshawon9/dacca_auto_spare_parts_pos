package com.daccaauto.pos.dto.sale;

import java.math.BigDecimal;

public record SaleResponse(
    Long id,
    String invoiceNo,
    BigDecimal total
) {
}
