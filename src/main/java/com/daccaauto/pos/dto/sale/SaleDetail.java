package com.daccaauto.pos.dto.sale;

import com.daccaauto.pos.entity.PaymentMethod;
import com.daccaauto.pos.entity.SaleType;
import com.daccaauto.pos.entity.VatMode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record SaleDetail(
    Long id,
    String invoiceNo,
    LocalDate saleDate,
    String customerName,
    String storeName,
    SaleType saleType,
    VatMode vatMode,
    BigDecimal vatPercent,
    PaymentMethod paymentMethod,
    BigDecimal subTotal,
    BigDecimal vatAmount,
    BigDecimal total,
    BigDecimal paidAmount,
    BigDecimal balanceDue,
    LocalDate dueDate,
    List<Line> lines
) {
    public record Line(
        Long id,
        Long productId,
        String productName,
        String categoryName,
        String brandName,
        String partNumber,
        BigDecimal quantity,
        BigDecimal unitPrice,
        BigDecimal costPrice,
        BigDecimal lineTotal
    ) {
    }
}
