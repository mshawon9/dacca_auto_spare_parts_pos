package com.daccaauto.pos.dto.vehicle;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VehicleModelCreateRequest {

    @NotNull
    private Long makeId;

    @NotBlank
    @Size(max = 100)
    private String name;

    private Boolean active = true;
}