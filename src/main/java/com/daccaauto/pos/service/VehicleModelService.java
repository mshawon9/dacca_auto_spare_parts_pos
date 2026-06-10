package com.daccaauto.pos.service;

import com.daccaauto.pos.dto.vehicle.*;

import java.util.List;

public interface VehicleModelService {
    VehicleModelResponse create(VehicleModelCreateRequest request);
    VehicleModelResponse update(Long id, VehicleModelUpdateRequest request);
    VehicleModelResponse getById(Long id);
    List<VehicleModelResponse> getAll(Long makeId);
    void delete(Long id);
}