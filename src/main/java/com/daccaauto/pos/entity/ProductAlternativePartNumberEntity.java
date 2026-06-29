package com.daccaauto.pos.entity;

import jakarta.persistence.*;
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
    name = "product_alternative_part_numbers",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_product_alternative_part_number",
            columnNames = {"product_id", "part_number"}
        )
    },
    indexes = {
        @Index(name = "idx_product_alt_part_product", columnList = "product_id"),
        @Index(name = "idx_product_alt_part_number", columnList = "part_number")
    }
)
public class ProductAlternativePartNumberEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private ProductEntity product;

    @NotBlank
    @Size(max = 100)
    @Pattern(
        regexp = "^[A-Za-z0-9._/\\- ]+$",
        message = "alternative part number contains unsupported characters"
    )
    @Column(name = "part_number", nullable = false, length = 100)
    private String partNumber;
}
