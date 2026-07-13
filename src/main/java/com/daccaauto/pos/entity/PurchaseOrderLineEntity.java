package com.daccaauto.pos.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "purchase_order_lines")
public class PurchaseOrderLineEntity extends BaseEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "purchase_order_id", nullable = false)
    private PurchaseOrderEntity purchaseOrder;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private ProductEntity product;

    @Size(max = 100)
    @Column(name = "supplier_product_code", length = 100)
    private String supplierProductCode;

    @NotNull
    @DecimalMin("1")
    @Column(nullable = false, precision = 19, scale = 3)
    private BigDecimal quantity;

    @NotNull
    @ColumnDefault("0")
    @Column(name = "returned_quantity", nullable = false, precision = 19, scale = 3)
    private BigDecimal returnedQuantity = BigDecimal.ZERO;

    @NotNull
    @DecimalMin("0.01")
    @Column(name = "unit_price", nullable = false, precision = 19, scale = 2)
    private BigDecimal unitPrice;

    @NotNull
    @Column(name = "tax_percent", nullable = false, precision = 7, scale = 2)
    private BigDecimal taxPercent = BigDecimal.ZERO;

    @NotNull
    @Column(name = "tax_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @NotNull
    @Column(name = "line_total", nullable = false, precision = 19, scale = 2)
    private BigDecimal lineTotal;
}
