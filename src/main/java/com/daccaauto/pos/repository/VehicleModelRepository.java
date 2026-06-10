package com.daccaauto.pos.repository;

import com.daccaauto.pos.entity.VehicleModelEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VehicleModelRepository extends JpaRepository<VehicleModelEntity, Long> {
    boolean existsByMakeIdAndNameIgnoreCase(Long makeId, String name);
    boolean existsByMakeIdAndNameIgnoreCaseAndIdNot(Long makeId, String name, Long id);
    List<VehicleModelEntity> findAllByOrderByNameAsc();
    List<VehicleModelEntity> findByMakeIdOrderByNameAsc(Long makeId);
}