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
    name = "stock_adjustments",
    indexes = {
        @Index(name = "idx_stock_adjustment_store_product", columnList = "store_id, product_id"),
        @Index(name = "idx_stock_adjustment_created_at", columnList = "created_at")
    }
)
public class StockAdjustmentEntity extends BaseEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private StoreEntity store;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private ProductEntity product;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "adjustment_type", nullable = false, length = 20)
    private StockAdjustmentType adjustmentType;

    @NotNull
    @Column(name = "previous_quantity", nullable = false, precision = 19, scale = 3)
    private BigDecimal previousQuantity;

    @NotNull
    @Column(name = "adjustment_quantity", nullable = false, precision = 19, scale = 3)
    private BigDecimal adjustmentQuantity;

    @NotNull
    @Column(name = "new_quantity", nullable = false, precision = 19, scale = 3)
    private BigDecimal newQuantity;

    @Size(max = 250)
    @Column(length = 250)
    private String note;
}
