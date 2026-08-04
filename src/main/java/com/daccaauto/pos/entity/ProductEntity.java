package com.daccaauto.pos.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Locale;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "products",
        indexes = {
                @Index(name = "idx_product_name", columnList = "name"),
                @Index(name = "idx_product_spec_label", columnList = "spec_label"),
                @Index(name = "idx_product_position", columnList = "position"),
                @Index(name = "idx_product_dimension", columnList = "dimension"),
                @Index(name = "idx_product_sku", columnList = "sku"),
                @Index(name = "idx_product_part_number", columnList = "part_number"),
                @Index(name = "idx_product_alternative_part_number", columnList = "alternative_part_number"),
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
    private String name;

    @Size(max = 120)
    @Column(name = "spec_label", length = 120)
    private String specLabel;

    @Convert(converter = ProductPositionConverter.class)
    @Column(name = "position", length = 80)
    private ProductPosition position;

    @Size(max = 120)
    @Column(name = "dimension", length = 120)
    private String dimension;

    @Size(max = 100)
    @Column(name = "sku", length = 100)
    private String sku;

    @NotBlank
    @Size(max = 100)
    @Pattern(
            regexp = "^[A-Za-z0-9._/\\-]+$",
            message = "partNumber contains unsupported characters"
    )
    @Column(name = "part_number", nullable = false, length = 100)
    private String partNumber;

    @Size(max = 255)
    @Pattern(
            regexp = "^[A-Za-z0-9._/\\- ,]*$",
            message = "alternativePartNumber contains unsupported characters"
    )
    @Column(name = "alternative_part_number", length = 255)
    private String alternativePartNumber;

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

    @Size(max = 100)
    @Column(name = "image_file_name", length = 100)
    private String imageFileName;

    @Size(max = 50)
    @Column(name = "image_content_type", length = 50)
    private String imageContentType;

    @DecimalMin("0.000")
    @Digits(integer = 16, fraction = 3)
    @Column(name = "reorder_level", precision = 19, scale = 3)
    private BigDecimal reorderLevel = BigDecimal.valueOf(2);

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "brand_id", nullable = false)
    private BrandEntity brand;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private ProductCategoryEntity category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_group_id")
    private ProductGroupEntity productGroup;

    @Column(nullable = false)
    private boolean active = true;

    @PrePersist
    @PreUpdate
    public void normalizePartNumber() {
        if (this.reorderLevel == null) {
            this.reorderLevel = BigDecimal.valueOf(2);
        }
        if (this.partNumber != null) {
            this.normalizedPartNumber = this.partNumber
                    .replaceAll("[\\s\\-_/\\.]", "")
                    .toUpperCase(Locale.ROOT);
        }
    }
}
