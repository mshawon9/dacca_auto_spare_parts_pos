package com.daccaauto.pos.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
    name = "product_price_history",
    indexes = {
        @Index(name = "idx_price_history_store_product", columnList = "store_id, product_id"),
        @Index(name = "idx_price_history_created_at", columnList = "created_at")
    }
)
public class ProductPriceHistoryEntity extends BaseEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private StoreEntity store;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private ProductEntity product;

    @Column(name = "old_price", precision = 19, scale = 2)
    private BigDecimal oldPrice;

    @NotNull
    @Column(name = "new_price", nullable = false, precision = 19, scale = 2)
    private BigDecimal newPrice;

    @Size(max = 250)
    @Column(length = 250)
    private String note;
}
