package com.daccaauto.pos.repository;

import com.daccaauto.pos.entity.PurchaseOrderEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrderEntity, Long> {

    boolean existsBySupplierIdAndInvoiceIdIgnoreCase(Long supplierId, String invoiceId);

    boolean existsBySupplierIdAndInvoiceIdIgnoreCaseAndIdNot(Long supplierId, String invoiceId, Long id);

    @Query("""
        select po
        from PurchaseOrderEntity po
        join po.supplier supplier
        join po.store store
        where (:keywordPattern is null
            or lower(po.invoiceId) like :keywordPattern
            or lower(supplier.name) like :keywordPattern
            or lower(store.name) like :keywordPattern
            or exists (
                select 1
                from PurchaseOrderLineEntity line
                join line.product product
                join product.brand brand
                where line.purchaseOrder = po
                    and (
                        lower(product.name) like :keywordPattern
                        or lower(product.partNumber) like :keywordPattern
                        or lower(brand.name) like :keywordPattern
                        or lower(line.supplierProductCode) like :keywordPattern
                    )
            ))
        order by po.purchaseDate desc, po.id desc
        """)
    Page<PurchaseOrderEntity> search(String keywordPattern, Pageable pageable);

    @EntityGraph(attributePaths = {
        "supplier",
        "store",
        "lines",
        "lines.product",
        "lines.product.brand",
        "lines.product.category"
    })
    Optional<PurchaseOrderEntity> findWithLinesById(Long id);
}
