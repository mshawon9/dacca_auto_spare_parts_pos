package com.daccaauto.pos.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "sales", uniqueConstraints = {
    @UniqueConstraint(name = "uk_sale_invoice_no", columnNames = "invoice_no")
})
public class SaleEntity extends BaseEntity {

    @NotNull
    @Column(name = "invoice_no", nullable = false, length = 80)
    private String invoiceNo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private CustomerEntity customer;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private StoreEntity store;

    @NotNull
    @Column(name = "sale_date", nullable = false)
    private LocalDate saleDate;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "sale_type", nullable = false, length = 30)
    private SaleType saleType;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "vat_mode", nullable = false, length = 20)
    private VatMode vatMode;

    @NotNull
    @Column(name = "sub_total", nullable = false, precision = 19, scale = 2)
    private BigDecimal subTotal = BigDecimal.ZERO;

    @NotNull
    @Column(name = "vat_percent", nullable = false, precision = 7, scale = 2)
    private BigDecimal vatPercent = BigDecimal.ZERO;

    @NotNull
    @Column(name = "vat_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal vatAmount = BigDecimal.ZERO;

    @NotNull
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    @NotNull
    @Enumerated(EnumType.STRING)
    @ColumnDefault("'CASH'")
    @Column(name = "payment_method", nullable = false, length = 30)
    private PaymentMethod paymentMethod = PaymentMethod.CASH;

    @NotNull
    @ColumnDefault("0")
    @Column(name = "paid_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal paidAmount = BigDecimal.ZERO;

    @NotNull
    @ColumnDefault("0")
    @Column(name = "balance_due", nullable = false, precision = 19, scale = 2)
    private BigDecimal balanceDue = BigDecimal.ZERO;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(length = 250)
    private String note;

    @OneToMany(mappedBy = "sale", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SaleLineEntity> lines = new ArrayList<>();
}
