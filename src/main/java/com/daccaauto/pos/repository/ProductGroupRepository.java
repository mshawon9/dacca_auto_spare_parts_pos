package com.daccaauto.pos.repository;

import com.daccaauto.pos.entity.ProductGroupEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProductGroupRepository extends JpaRepository<ProductGroupEntity, Long> {

    Optional<ProductGroupEntity> findByCategoryIdAndNormalizedKey(Long categoryId, String normalizedKey);
}
