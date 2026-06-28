package com.daccaauto.pos.repository;

import com.daccaauto.pos.entity.VehicleMakeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VehicleMakeRepository extends JpaRepository<VehicleMakeEntity, Long> {
    boolean existsByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
    List<VehicleMakeEntity> findAllByOrderByNameAsc();
    Optional<VehicleMakeEntity> findByNameIgnoreCase(String name);
}
