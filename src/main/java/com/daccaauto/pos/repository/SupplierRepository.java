package com.daccaauto.pos.repository;

import com.daccaauto.pos.entity.SupplierEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SupplierRepository extends JpaRepository<SupplierEntity, Long> {

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    boolean existsByTrnNumberIgnoreCase(String trnNumber);

    boolean existsByTrnNumberIgnoreCaseAndIdNot(String trnNumber, Long id);

    List<SupplierEntity> findAllByOrderByNameAsc();
}