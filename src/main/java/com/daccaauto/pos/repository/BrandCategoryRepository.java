package com.daccaauto.pos.repository;

import com.daccaauto.pos.entity.BrandCategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BrandCategoryRepository extends JpaRepository<BrandCategoryEntity, Long> {

    boolean existsByBrandIdAndCategoryId(Long brandId, Long categoryId);

    boolean existsByBrandIdAndCategoryIdAndIdNot(Long brandId, Long categoryId, Long id);

    boolean existsByBrandIdAndCategoryIdAndActiveTrue(Long brandId, Long categoryId);

    List<BrandCategoryEntity> findByCategoryIdAndActiveTrueOrderByDisplayOrderAscBrandNameAsc(Long categoryId);

    List<BrandCategoryEntity> findByBrandIdAndActiveTrueOrderByCategoryNameAsc(Long brandId);

    void deleteByCategoryId(Long categoryId);
}