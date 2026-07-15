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
@Table(name = "sale_drafts", indexes = {
    @Index(name = "idx_sale_draft_updated_at", columnList = "updated_at")
})
public class SaleDraftEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private CustomerEntity customer;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private StoreEntity store;

    @NotNull
    @Column(name = "sale_date", nullable = false)
    private LocalDate saleDate = LocalDate.now();

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "sale_type", nullable = false, length = 30)
    private SaleType saleType = SaleType.REGULAR;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "vat_mode", nullable = false, length = 20)
    private VatMode vatMode = VatMode.EXCLUSIVE;

    @NotNull
    @Column(name = "vat_percent", nullable = false, precision = 7, scale = 2)
    private BigDecimal vatPercent = BigDecimal.ZERO;

    @NotNull
    @Enumerated(EnumType.STRING)
    @ColumnDefault("'CASH'")
    @Column(name = "payment_method", nullable = false, length = 30)
    private PaymentMethod paymentMethod = PaymentMethod.CASH;

    @NotNull
    @ColumnDefault("0")
    @Column(name = "paid_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal paidAmount = BigDecimal.ZERO;

    @Column(length = 250)
    private String note;

    @OneToMany(mappedBy = "draft", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SaleDraftLineEntity> lines = new ArrayList<>();

    @OneToMany(mappedBy = "draft", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SaleDraftActionEntity> actions = new ArrayList<>();
}
