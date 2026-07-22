package com.daccaauto.pos.dto.sale;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class SaleInvoicePdfLine {

    private Integer sno;
    private String productName;
    private String partNumber;
    private BigDecimal quantity;
    private BigDecimal unitPrice;
    private BigDecimal subTotal;
    private BigDecimal vatPercent;
    private BigDecimal vatAmount;
    private BigDecimal lineTotal;
}
