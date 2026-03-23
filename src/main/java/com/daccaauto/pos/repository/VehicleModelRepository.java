package com.daccaauto.pos.repository;

import com.daccaauto.pos.entity.VehicleModelEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VehicleModelRepository extends JpaRepository<VehicleModelEntity, Long> {
    List<VehicleModelEntity> findByMakeIdAndActiveTrueOrderByNameAsc(Long makeId);
}