package com.daccaauto.pos.dto.sale;

import com.daccaauto.pos.entity.SaleType;
import com.daccaauto.pos.entity.VatMode;
import com.daccaauto.pos.entity.PaymentMethod;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record SaleDraftResponse(
    Long id,
    Long customerId,
    String customerName,
    Long storeId,
    String storeName,
    LocalDate saleDate,
    SaleType saleType,
    VatMode vatMode,
    BigDecimal vatPercent,
    String note,
    BigDecimal subTotal,
    BigDecimal vatAmount,
    BigDecimal total,
    PaymentMethod paymentMethod,
    BigDecimal paidAmount,
    BigDecimal balanceDue,
    List<Line> lines
) {
    public record Line(
        Long id,
        Long productId,
        String productText,
        BigDecimal quantity,
        BigDecimal unitPrice,
        BigDecimal costPrice,
        BigDecimal vatAmount,
        BigDecimal lineTotal
    ) {
    }
}
