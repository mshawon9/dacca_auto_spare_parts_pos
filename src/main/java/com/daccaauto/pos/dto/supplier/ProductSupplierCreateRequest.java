package com.daccaauto.pos.dto.supplier;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductSupplierCreateRequest {

    @NotNull
    private Long productId;

    @NotNull
    private Long supplierId;

    @Size(max = 100)
    private String supplierProductCode;

    @NotBlank
    @Size(max = 60)
    private String priceInput;

    @Size(max = 10)
    private String currency;

    @Size(max = 500)
    private String notes;

    private Boolean active = true;
}