package com.daccaauto.pos.repository;

import com.daccaauto.pos.entity.BrandEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BrandRepository extends JpaRepository<BrandEntity, Long> {
    boolean existsByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
    Optional<BrandEntity> findByNameIgnoreCase(String name);
    List<BrandEntity> findAllByOrderByNameAsc();
}
