package com.daccaauto.pos.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
    name = "vehicle_application",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_vehicle_application_unique",
            columnNames = {
                "vehicle_make_id",
                "vehicle_model_id",
                "variant_label",
                "year_from",
                "year_to"
            }
        )
    },
    indexes = {
        @Index(name = "idx_vehicle_application_display_name", columnList = "display_name")
    }
)
public class VehicleApplicationEntity extends BaseEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vehicle_make_id", nullable = false)
    private VehicleMakeEntity vehicleMake;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vehicle_model_id", nullable = false)
    private VehicleModelEntity vehicleModel;

    @Size(max = 100)
    @Column(name = "variant_label", length = 100)
    private String variantLabel; // India, Japan, Korean etc.

    @Min(1950)
    @Max(2100)
    @Column(name = "year_from")
    private Integer yearFrom;

    @Min(1950)
    @Max(2100)
    @Column(name = "year_to")
    private Integer yearTo; // null means Up

    @NotBlank
    @Size(max = 150)
    @Column(name = "display_name", nullable = false, length = 150)
    private String displayName;

    @Column(nullable = false)
    private boolean active = true;

    @AssertTrue(message = "yearFrom must be less than or equal to yearTo")
    public boolean isYearRangeValid() {
        return yearFrom == null || yearTo == null || yearFrom <= yearTo;
    }

    @PrePersist
    @PreUpdate
    public void buildDisplayName() {
        String modelPart = vehicleModel != null ? vehicleModel.getName() : "";
        String variantPart = (variantLabel != null && !variantLabel.isBlank()) ? " " + variantLabel.trim() : "";

        if (yearFrom == null && yearTo == null) {
            this.displayName = (modelPart + variantPart).trim();
        } else if (yearFrom != null && yearTo == null) {
            this.displayName = (modelPart + variantPart + " " + yearFrom + " Up").trim();
        } else if (yearFrom != null && yearFrom.equals(yearTo)) {
            this.displayName = (modelPart + variantPart + " " + yearFrom).trim();
        } else {
            this.displayName = (modelPart + variantPart + " " + yearFrom + "-" + yearTo).trim();
        }
    }
}