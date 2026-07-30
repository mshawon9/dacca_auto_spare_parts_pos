package com.daccaauto.pos.dto.store;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StoreManageRequest {

    @NotBlank
    @Size(max = 120)
    private String name;

    @Size(max = 50)
    private String code;

    @Size(max = 250)
    private String address;

    private Boolean active = true;
}
