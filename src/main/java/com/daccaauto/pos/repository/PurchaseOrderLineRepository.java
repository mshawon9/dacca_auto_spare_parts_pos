package com.daccaauto.pos.repository;

import com.daccaauto.pos.entity.PurchaseOrderLineEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PurchaseOrderLineRepository extends JpaRepository<PurchaseOrderLineEntity, Long> {

    @EntityGraph(attributePaths = {
        "purchaseOrder",
        "purchaseOrder.store",
        "product"
    })
    Optional<PurchaseOrderLineEntity> findWithPurchaseOrderAndProductById(Long id);
}
