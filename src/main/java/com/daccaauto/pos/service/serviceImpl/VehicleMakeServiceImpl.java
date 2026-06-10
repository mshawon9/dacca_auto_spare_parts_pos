package com.daccaauto.pos.service.serviceImpl;

import com.daccaauto.pos.dto.vehicle.VehicleMakeCreateRequest;
import com.daccaauto.pos.dto.vehicle.VehicleMakeResponse;
import com.daccaauto.pos.dto.vehicle.VehicleMakeUpdateRequest;
import com.daccaauto.pos.entity.VehicleMakeEntity;
import com.daccaauto.pos.exception.DuplicateResourceException;
import com.daccaauto.pos.exception.ResourceNotFoundException;
import com.daccaauto.pos.repository.VehicleMakeRepository;
import com.daccaauto.pos.service.VehicleMakeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class VehicleMakeServiceImpl implements VehicleMakeService {

    private final VehicleMakeRepository vehicleMakeRepository;

    @Override
    public VehicleMakeResponse create(VehicleMakeCreateRequest request) {
        String name = request.getName().trim();

        if (vehicleMakeRepository.existsByNameIgnoreCase(name)) {
            throw new DuplicateResourceException("Vehicle make already exists: " + name);
        }

        VehicleMakeEntity entity = new VehicleMakeEntity();
        entity.setName(name);
        entity.setActive(request.getActive() == null || request.getActive());

        return map(vehicleMakeRepository.save(entity));
    }

    @Override
    public VehicleMakeResponse update(Long id, VehicleMakeUpdateRequest request) {
        VehicleMakeEntity entity = vehicleMakeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle make not found: " + id));

        String name = request.getName().trim();

        if (vehicleMakeRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new DuplicateResourceException("Vehicle make already exists: " + name);
        }

        entity.setName(name);
        entity.setActive(request.getActive() == null || request.getActive());

        return map(vehicleMakeRepository.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public VehicleMakeResponse getById(Long id) {
        return map(
                vehicleMakeRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Vehicle make not found: " + id))
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<VehicleMakeResponse> getAll() {
        return vehicleMakeRepository.findAllByOrderByNameAsc()
                .stream()
                .map(this::map)
                .toList();
    }

    @Override
    public void delete(Long id) {
        VehicleMakeEntity entity = vehicleMakeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle make not found: " + id));

        vehicleMakeRepository.delete(entity);
    }

    private VehicleMakeResponse map(VehicleMakeEntity entity) {
        return new VehicleMakeResponse(
                entity.getId(),
                entity.getName(),
                entity.isActive()
        );
    }
}