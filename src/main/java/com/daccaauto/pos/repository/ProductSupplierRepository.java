package com.daccaauto.pos.repository;

import com.daccaauto.pos.entity.ProductSupplierEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductSupplierRepository extends JpaRepository<ProductSupplierEntity, Long> {

    boolean existsByProductIdAndSupplierId(Long productId, Long supplierId);

    boolean existsByProductIdAndSupplierIdAndIdNot(Long productId, Long supplierId, Long id);

    List<ProductSupplierEntity> findByProductIdOrderBySupplierNameAsc(Long productId);

    List<ProductSupplierEntity> findBySupplierIdOrderByProductNameAsc(Long supplierId);
}