package com.daccaauto.pos.dto.vehicle;

public record VehicleMakeResponse(
    Long id,
    String name,
    boolean active
) {
}