package com.daccaauto.pos.dto.purchase;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class PurchaseReturnRequest {

    @NotNull
    private Long lineId;

    @NotNull
    private BigDecimal quantity;
}
