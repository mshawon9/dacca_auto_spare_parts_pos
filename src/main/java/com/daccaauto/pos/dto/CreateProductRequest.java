package com.daccaauto.pos.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
public class CreateProductRequest {

    @NotBlank
    @Size(max = 200)
    private String name;

    @Size(max = 120)
    private String specLabel;

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
    private Long brandId;

    @NotNull
    private Long categoryId;

    private Set<Long> applicationIds;
}