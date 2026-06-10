package com.daccaauto.pos.dto.product;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashSet;
import java.util.Set;

@Getter
@Setter
public class ProductUpdateRequest {

    @NotBlank
    @Size(max = 200)
    private String name;

    @Size(max = 120)
    private String specLabel;

    @Size(max = 120)
    private String dimension;

    @Size(max = 100)
    private String sku;

    @NotBlank
    @Size(max = 100)
    @Pattern(regexp = "^[A-Za-z0-9._/\\- ]+$")
    private String partNumber;

    @Size(max = 64)
    @Pattern(regexp = "^[A-Za-z0-9\\-]*$")
    private String barcode;

    @Size(max = 2000)
    private String description;

    @NotNull
    private Long categoryId;

    @NotNull
    private Long brandId;

    private Set<Long> applicationIds = new LinkedHashSet<>();

    private Boolean active = true;
}