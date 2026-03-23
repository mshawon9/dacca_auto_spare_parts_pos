package com.daccaauto.pos.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "product",
        indexes = {
                @Index(name = "idx_product_name", columnList = "name"),
                @Index(name = "idx_product_spec_label", columnList = "spec_label"),
                @Index(name = "idx_product_part_number", columnList = "part_number"),
                @Index(name = "idx_product_normalized_part_number", columnList = "normalized_part_number"),
                @Index(name = "idx_product_barcode", columnList = "barcode")
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_product_brand_normalized_part_number",
                        columnNames = {"brand_id", "normalized_part_number"}
                )
        }
)
public class ProductEntity extends BaseEntity {

    @NotBlank
    @Size(max = 200)
    @Column(nullable = false, length = 200)
    private String name; // Brake Pad, Bearing, Shock Absorber

    @Size(max = 120)
    @Column(name = "spec_label", length = 120)
    private String specLabel; // Front, Rear, LH, RH, 38 X 25 X 9

    @Size(max = 100)
    @Column(name = "sku", length = 100)
    private String sku; // internal code, optional

    @NotBlank
    @Size(max = 100)
    @Pattern(
            regexp = "^[A-Za-z0-9._/\\- ]+$",
            message = "partNumber contains unsupported characters"
    )
    @Column(name = "part_number", nullable = false, length = 100)
    private String partNumber;

    @NotBlank
    @Size(max = 100)
    @Column(name = "normalized_part_number", nullable = false, length = 100)
    private String normalizedPartNumber;

    @Size(max = 64)
    @Pattern(
            regexp = "^[A-Za-z0-9\\-]*$",
            message = "barcode must be alphanumeric or hyphen"
    )
    @Column(length = 64)
    private String barcode;

    @Size(max = 2000)
    @Column(length = 2000)
    private String description;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "brand_id", nullable = false)
    private BrandEntity brand;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private ProductCategoryEntity category;

    @Size(max = 120)
    @Column(name = "dimension", length = 120)
    private String dimension; // 38 X 25 X 9

    @Column(nullable = false)
    private boolean active = true;

    @OneToMany(
            mappedBy = "product",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private Set<ProductApplicationEntity> applications = new LinkedHashSet<>();

    @PrePersist
    @PreUpdate
    public void normalizePartNumber() {
        if (this.partNumber != null) {
            this.normalizedPartNumber = this.partNumber
                    .replaceAll("[\\s\\-_/\\.]", "")
                    .toUpperCase(Locale.ROOT);
        }
    }

    public void addApplication(ProductApplicationEntity productApplicationEntity) {
        productApplicationEntity.setProduct(this);
        this.applications.add(productApplicationEntity);
    }
}