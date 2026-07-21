package com.daccaauto.pos.dto.sale;

import com.daccaauto.pos.entity.PaymentMethod;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SalePaymentRow(
    Long id,
    LocalDate receiveDate,
    BigDecimal amount,
    PaymentMethod paymentMethod,
    LocalDate chequeDate,
    String chequeNumber,
    String note
) {
}
