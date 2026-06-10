package com.daccaauto.pos.service;

import com.daccaauto.pos.dto.vehicle.*;

import java.util.List;

public interface VehicleMakeService {
    VehicleMakeResponse create(VehicleMakeCreateRequest request);
    VehicleMakeResponse update(Long id, VehicleMakeUpdateRequest request);
    VehicleMakeResponse getById(Long id);
    List<VehicleMakeResponse> getAll();
    void delete(Long id);
}