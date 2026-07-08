package com.daccaauto.pos.repository;

import com.daccaauto.pos.entity.StoreEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StoreRepository extends JpaRepository<StoreEntity, Long> {

    boolean existsByNameIgnoreCase(String name);

    long countByActiveTrue();

    Optional<StoreEntity> findByNameIgnoreCase(String name);

    List<StoreEntity> findAllByActiveTrueOrderByNameAsc();
}
