package com.daccaauto.pos.repository;

import com.daccaauto.pos.entity.ProductAlternativePartNumberEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductAlternativePartNumberRepository extends JpaRepository<ProductAlternativePartNumberEntity, Long> {

    void deleteByProductId(Long productId);

    List<ProductAlternativePartNumberEntity> findByProductIdOrderByPartNumberAsc(Long productId);
}
