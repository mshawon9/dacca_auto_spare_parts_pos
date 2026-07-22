package com.daccaauto.pos.dto.sale;

import com.daccaauto.pos.entity.PaymentMethod;
import com.daccaauto.pos.entity.SaleType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CustomerLedgerRow(
    LocalDate entryDate,
    String entryType,
    String reference,
    String description,
    SaleType saleType,
    PaymentMethod paymentMethod,
    LocalDate chequeDate,
    String chequeNumber,
    BigDecimal debit,
    BigDecimal credit,
    BigDecimal balance
) {
}
