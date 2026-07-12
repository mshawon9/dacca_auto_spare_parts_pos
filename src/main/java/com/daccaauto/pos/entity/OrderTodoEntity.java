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
    name = "order_todos",
    indexes = {
        @Index(name = "idx_order_todo_product", columnList = "product_id"),
        @Index(name = "idx_order_todo_status", columnList = "status")
    }
)
public class OrderTodoEntity extends BaseEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private ProductEntity product;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OrderTodoStatus status = OrderTodoStatus.PENDING;

    @NotNull
    @Column(name = "current_quantity", nullable = false, precision = 19, scale = 3)
    private BigDecimal currentQuantity = BigDecimal.ZERO;

    @NotNull
    @Column(name = "reorder_level", nullable = false, precision = 19, scale = 3)
    private BigDecimal reorderLevel = BigDecimal.valueOf(2);

    @Size(max = 250)
    @Column(length = 250)
    private String note;
}
