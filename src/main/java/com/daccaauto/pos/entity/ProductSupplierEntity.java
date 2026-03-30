package com.daccaauto.pos.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
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
    name = "product_suppliers",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_product_supplier",
            columnNames = {"product_id", "supplier_id"}
        )
    }
)
public class ProductSupplierEntity extends BaseEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private ProductEntity product;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "supplier_id", nullable = false)
    private SupplierEntity supplier;

    @Size(max = 100)
    @Column(name = "supplier_product_code", length = 100)
    private String supplierProductCode;

    @NotBlank
    @Size(max = 60)
    @Column(name = "price_input", nullable = false, length = 60)
    private String priceInput;

    @DecimalMin(value = "0.0", inclusive = false)
    @Column(name = "price_value", precision = 18, scale = 4)
    private BigDecimal priceValue;

    @Size(max = 10)
    @Column(length = 10)
    private String currency;

    @Size(max = 500)
    @Column(length = 500)
    private String notes;

    @Column(nullable = false)
    private boolean active = true;
}