package com.daccaauto.pos.dto.vehicle;

public record VehicleApplicationResponse(
    Long id,
    Long vehicleMakeId,
    String vehicleMakeName,
    Long vehicleModelId,
    String vehicleModelName,
    String variantLabel,
    Integer yearFrom,
    Integer yearTo,
    String displayName,
    boolean active
) {
}