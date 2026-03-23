package com.daccaauto.pos.repository;

import com.daccaauto.pos.entity.VehicleMakeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VehicleMakeRepository extends JpaRepository<VehicleMakeEntity, Long> {
}