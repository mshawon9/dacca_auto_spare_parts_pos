package com.daccaauto.pos.dto.vehicle;

public record VehicleModelResponse(
    Long id,
    Long makeId,
    String makeName,
    String name,
    boolean active
) {
}