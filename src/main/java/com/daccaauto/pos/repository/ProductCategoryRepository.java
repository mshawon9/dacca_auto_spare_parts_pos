package com.daccaauto.pos.repository;

import com.daccaauto.pos.entity.ProductCategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductCategoryRepository extends JpaRepository<ProductCategoryEntity, Long> {
    boolean existsByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
    Optional<ProductCategoryEntity> findByNameIgnoreCase(String name);

    List<ProductCategoryEntity> findAllByOrderByNameAsc();
}
