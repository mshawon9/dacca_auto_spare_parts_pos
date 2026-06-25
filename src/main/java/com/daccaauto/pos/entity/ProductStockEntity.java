package com.daccaauto.pos.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
    name = "product_stocks",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_product_stock_store_product",
            columnNames = {"store_id", "product_id"}
        )
    },
    indexes = {
        @Index(name = "idx_product_stock_store", columnList = "store_id"),
        @Index(name = "idx_product_stock_product", columnList = "product_id")
    }
)
public class ProductStockEntity extends BaseEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private StoreEntity store;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private ProductEntity product;

    @NotNull
    @DecimalMin("0.000")
    @Column(nullable = false, precision = 19, scale = 3)
    private BigDecimal quantity = BigDecimal.ZERO;

    @Column(name = "selling_price", precision = 19, scale = 2)
    private BigDecimal sellingPrice;
}
