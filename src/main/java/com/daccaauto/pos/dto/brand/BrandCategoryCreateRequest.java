package com.daccaauto.pos.dto.brand;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BrandCategoryCreateRequest {

    @NotNull
    private Long brandId;

    @NotNull
    private Long categoryId;

    private Integer displayOrder;

    private Boolean active = true;
}