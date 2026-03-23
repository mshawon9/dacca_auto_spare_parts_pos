package com.daccaauto.pos.entity;

import jakarta.persistence.*;
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
        name = "product_application",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_product_application_unique",
                        columnNames = {"product_id", "vehicle_application_id"}
                )
        }
)
public class ProductApplicationEntity extends BaseEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private ProductEntity product;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vehicle_application_id", nullable = false)
    private VehicleApplicationEntity vehicleApplication;

    @Size(max = 500)
    @Column(length = 500)
    private String notes;
}