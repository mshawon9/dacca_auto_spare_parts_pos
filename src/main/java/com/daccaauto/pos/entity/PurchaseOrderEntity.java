package com.daccaauto.pos.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
    name = "purchase_orders",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_purchase_supplier_invoice", columnNames = {"supplier_id", "invoice_id"})
    }
)
public class PurchaseOrderEntity extends BaseEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "supplier_id", nullable = false)
    private SupplierEntity supplier;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private StoreEntity store;

    @NotNull
    @Column(name = "purchase_date", nullable = false)
    private LocalDate purchaseDate;

    @NotBlank
    @Size(max = 80)
    @Column(name = "invoice_id", nullable = false, length = 80)
    private String invoiceId;

    @Size(max = 150)
    @Column(name = "invoice_supplier_name", length = 150)
    private String invoiceSupplierName;
    @Size(max = 100)
    @Column(name = "invoice_supplier_contact_person", length = 100)
    private String invoiceSupplierContactPerson;
    @Size(max = 30)
    @Column(name = "invoice_supplier_phone", length = 30)
    private String invoiceSupplierPhone;
    @Size(max = 120)
    @Column(name = "invoice_supplier_email", length = 120)
    private String invoiceSupplierEmail;
    @Size(max = 255)
    @Column(name = "invoice_supplier_address", length = 255)
    private String invoiceSupplierAddress;
    @Size(max = 50)
    @Column(name = "invoice_supplier_trn_number", length = 50)
    private String invoiceSupplierTrnNumber;

    @NotNull
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    @OneToMany(mappedBy = "purchaseOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PurchaseOrderLineEntity> lines = new ArrayList<>();
}
