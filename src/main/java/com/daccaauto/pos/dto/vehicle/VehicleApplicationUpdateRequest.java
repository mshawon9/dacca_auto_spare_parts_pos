package com.daccaauto.pos.dto.vehicle;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VehicleApplicationUpdateRequest {

    private Long vehicleMakeId;

    private Long vehicleModelId;

    @Size(max = 100)
    private String vehicleMakeName;

    @Size(max = 100)
    private String vehicleModelName;

    @Size(max = 100)
    private String variantLabel;

    @Min(1950)
    @Max(2100)
    private Integer yearFrom;

    @Min(1950)
    @Max(2100)
    private Integer yearTo;

    private Boolean active = true;

    @AssertTrue(message = "yearFrom must be <= yearTo")
    public boolean isYearRangeValid() {
        return yearFrom == null || yearTo == null || yearFrom <= yearTo;
    }

    @AssertTrue(message = "Vehicle make is required")
    public boolean isVehicleMakeProvided() {
        return vehicleMakeId != null || (vehicleMakeName != null && !vehicleMakeName.isBlank());
    }

    @AssertTrue(message = "Vehicle model is required")
    public boolean isVehicleModelProvided() {
        return vehicleModelId != null || (vehicleModelName != null && !vehicleModelName.isBlank());
    }
}
