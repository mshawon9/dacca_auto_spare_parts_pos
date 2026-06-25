package com.daccaauto.pos.repository;

import com.daccaauto.pos.entity.StoreEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StoreRepository extends JpaRepository<StoreEntity, Long> {

    boolean existsByNameIgnoreCase(String name);

    long countByActiveTrue();

    List<StoreEntity> findAllByActiveTrueOrderByNameAsc();
}
