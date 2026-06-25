package com.daccaauto.pos.dto.inventory;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record StoreCreateRequest(
    @NotBlank @Size(max = 120) String name,
    @Size(max = 50) String code,
    @Size(max = 250) String address
) {
}
