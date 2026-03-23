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
    name = "vehicle_make",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_vehicle_make_name", columnNames = "name")
    }
)
public class VehicleMakeEntity extends BaseEntity {

    @NotBlank
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String name; // Honda, Nissan, Toyota

    @Column(nullable = false)
    private boolean active = true;
}