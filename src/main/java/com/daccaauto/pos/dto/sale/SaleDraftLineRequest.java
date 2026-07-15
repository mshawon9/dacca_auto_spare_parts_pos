package com.daccaauto.pos.dto.sale;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class SaleDraftLineRequest {
    private Long productId;
    private BigDecimal quantity;
    private BigDecimal unitPrice;
}
