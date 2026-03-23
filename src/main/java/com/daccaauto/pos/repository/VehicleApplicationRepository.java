package com.daccaauto.pos.repository;

import com.daccaauto.pos.entity.VehicleApplicationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VehicleApplicationRepository extends JpaRepository<VehicleApplicationEntity, Long> {
}