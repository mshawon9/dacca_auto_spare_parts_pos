package com.daccaauto.pos.service;

import com.daccaauto.pos.dto.vehicle.*;

import java.util.List;

public interface VehicleApplicationService {

    VehicleApplicationResponse create(VehicleApplicationCreateRequest request);

    VehicleApplicationResponse update(Long id, VehicleApplicationUpdateRequest request);

    VehicleApplicationResponse getById(Long id);

    List<VehicleApplicationResponse> getAll(Long makeId, Long modelId, String keyword);

    void delete(Long id);
}