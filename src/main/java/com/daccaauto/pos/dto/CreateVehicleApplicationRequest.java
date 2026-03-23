package com.daccaauto.pos.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateVehicleApplicationRequest {

    @NotNull
    private Long vehicleMakeId;

    @NotNull
    private Long vehicleModelId;

    @Size(max = 100)
    private String variantLabel;

    @Min(1950)
    @Max(2100)
    private Integer yearFrom;

    @Min(1950)
    @Max(2100)
    private Integer yearTo;

    @AssertTrue(message = "yearFrom must be <= yearTo")
    public boolean isYearRangeValid() {
        return yearFrom == null || yearTo == null || yearFrom <= yearTo;
    }
}