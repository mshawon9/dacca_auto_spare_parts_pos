package com.daccaauto.pos.dto.purchase;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class PurchaseOrderRequest {

    private Long id;

    @NotNull
    private Long supplierId;

    @NotNull
    private Long storeId;

    @NotNull
    private LocalDate purchaseDate = LocalDate.now();

    @NotBlank
    @Size(max = 80)
    private String invoiceId;

    @Valid
    private List<Line> lines = new ArrayList<>();

    @Getter
    @Setter
    public static class Line {
        private Long id;

        private Long productId;

        private String productText;

        @Size(max = 100)
        private String supplierProductCode;

        private BigDecimal quantity;

        private BigDecimal unitPrice;

        private BigDecimal taxPercent;
    }
}
