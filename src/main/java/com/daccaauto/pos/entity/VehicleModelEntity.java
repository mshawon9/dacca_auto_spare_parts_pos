package com.daccaauto.pos.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
    name = "vehicle_model",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_vehicle_model_make_name",
            columnNames = {"make_id", "name"}
        )
    },
    indexes = {
        @Index(name = "idx_vehicle_model_name", columnList = "name")
    }
)
public class VehicleModelEntity extends BaseEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "make_id", nullable = false)
    private VehicleMakeEntity make;

    @NotBlank
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String name; // Civic, Sunny, Yaris

    @Column(nullable = false)
    private boolean active = true;
}