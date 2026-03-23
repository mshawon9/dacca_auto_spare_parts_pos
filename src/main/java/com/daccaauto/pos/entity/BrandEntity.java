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
    name = "brand",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_brand_name", columnNames = "name")
    }
)
public class BrandEntity extends BaseEntity {

    @NotBlank
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String name;

    @Size(max = 30)
    @Column(length = 30)
    private String code;

    @Column(nullable = false)
    private boolean active = true;
}