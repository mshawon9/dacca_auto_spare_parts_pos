package com.daccaauto.pos.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
    name = "manufacturers",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_manufacturer_name", columnNames = "name")
    }
)
public class ManufacturerEntity extends BaseEntity {

    @NotBlank
    @Size(max = 120)
    @Column(nullable = false, length = 120)
    private String name;

    @Size(max = 100)
    @Column(length = 100)
    private String country;
}