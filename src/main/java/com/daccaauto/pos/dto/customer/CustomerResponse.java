package com.daccaauto.pos.dto.customer;

public record CustomerResponse(
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
