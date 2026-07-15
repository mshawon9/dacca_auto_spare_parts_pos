package com.daccaauto.pos.dto.sale;

import com.daccaauto.pos.entity.SaleType;
import com.daccaauto.pos.entity.VatMode;
import com.daccaauto.pos.entity.PaymentMethod;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class SaleDraftHeaderRequest {
    private Long customerId;
    private Long storeId;
    private LocalDate saleDate;
    private SaleType saleType;
    private VatMode vatMode;
    private BigDecimal vatPercent;
    private PaymentMethod paymentMethod;
    private BigDecimal paidAmount;
    private String note;
}
