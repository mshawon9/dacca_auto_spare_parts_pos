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
    name = "product_groups",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_product_group_category_key",
            columnNames = {"category_id", "normalized_key"}
        )
    },
    indexes = {
        @Index(name = "idx_product_group_category", columnList = "category_id"),
        @Index(name = "idx_product_group_key", columnList = "normalized_key")
    }
)
public class ProductGroupEntity extends BaseEntity {

    @NotBlank
    @Size(max = 200)
    @Column(nullable = false, length = 200)
    private String name;

    @NotBlank
    @Size(max = 250)
    @Column(name = "normalized_key", nullable = false, length = 250)
    private String normalizedKey;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private ProductCategoryEntity category;
}
