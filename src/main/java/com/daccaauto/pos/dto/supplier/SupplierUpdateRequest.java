package com.daccaauto.pos.dto.supplier;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SupplierUpdateRequest {

    @NotBlank
    @Size(max = 150)
    private String name;

    @Size(max = 100)
    private String contactPerson;

    @Size(max = 30)
    private String phone;

    @Email
    @Size(max = 120)
    private String email;

    @Size(max = 255)
    private String address;

    @Size(max = 50)
    @Pattern(
        regexp = "^[A-Za-z0-9\\-]*$",
        message = "TRN number must be alphanumeric or hyphen"
    )
    private String trnNumber;

    private Boolean active = true;
}