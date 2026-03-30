package com.daccaauto.pos.dto.brand;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BrandCreateRequest {

    @NotBlank
    @Size(max = 100)
    private String name;

    @Size(max = 30)
    private String code;

    private Boolean active = true;
}