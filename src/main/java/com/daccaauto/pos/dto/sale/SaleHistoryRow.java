package com.daccaauto.pos.dto.sale;

import com.daccaauto.pos.entity.PaymentMethod;
import com.daccaauto.pos.entity.SaleType;
import com.daccaauto.pos.entity.VatMode;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SaleHistoryRow(
    Long id,
    String invoiceNo,
    LocalDate saleDate,
    String customerName,
    String storeName,
    SaleType saleType,
    VatMode vatMode,
    PaymentMethod paymentMethod,
    int lineCount,
    BigDecimal subTotal,
    BigDecimal vatAmount,
    BigDecimal total,
    BigDecimal paidAmount,
    BigDecimal balanceDue
) {
}
