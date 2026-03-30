package com.daccaauto.pos.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
    name = "suppliers",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_supplier_name", columnNames = "name"),
        @UniqueConstraint(name = "uk_supplier_trn_number", columnNames = "trn_number")
    }
)
public class SupplierEntity extends BaseEntity {

    @NotBlank
    @Size(max = 150)
    @Column(nullable = false, length = 150)
    private String name;

    @Size(max = 100)
    @Column(name = "contact_person", length = 100)
    private String contactPerson;

    @Size(max = 30)
    @Column(length = 30)
    private String phone;

    @Email
    @Size(max = 120)
    @Column(length = 120)
    private String email;

    @Size(max = 255)
    @Column(length = 255)
    private String address;

    @Size(max = 50)
    @Pattern(
        regexp = "^[A-Za-z0-9\\-]*$",
        message = "TRN number must be alphanumeric or hyphen"
    )
    @Column(name = "trn_number", length = 50)
    private String trnNumber;

    @Column(nullable = false)
    private boolean active = true;
}