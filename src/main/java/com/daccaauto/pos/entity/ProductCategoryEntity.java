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
    name = "product_category",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_product_category_name", columnNames = "name")
    }
)
public class ProductCategoryEntity extends BaseEntity {

    public String getName() {
        return name == null ? null : name.trim().toUpperCase(java.util.Locale.ROOT);
    }

    public void setName(String name) {
        this.name = name == null ? null : name.trim().toUpperCase(java.util.Locale.ROOT);
    }

    @PrePersist
    @PreUpdate
    private void normalizeName() {
        setName(name);
    }

    @NotBlank
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String name; // Brake Pad, Shock Absorber, Bearing

    @Size(max = 255)
    private String description;

    @Column(nullable = false)
    private boolean active = true;
}
