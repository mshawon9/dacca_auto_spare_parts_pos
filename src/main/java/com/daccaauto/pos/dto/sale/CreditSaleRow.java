package com.daccaauto.pos.dto.sale;

import com.daccaauto.pos.entity.SaleType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreditSaleRow(
    Long id,
    String invoiceNo,
    LocalDate saleDate,
    String customerName,
    String storeName,
    SaleType saleType,
    BigDecimal total,
    BigDecimal paidAmount,
    BigDecimal balanceDue,
    LocalDate dueDate,
    boolean overdue
) {
}
