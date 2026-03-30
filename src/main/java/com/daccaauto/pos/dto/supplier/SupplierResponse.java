package com.daccaauto.pos.dto.supplier;

public record SupplierResponse(
    Long id,
    String name,
    String contactPerson,
    String phone,
    String email,
    String address,
    String trnNumber,
    boolean active
) {
}