package com.daccaauto.pos.service.serviceImpl;

import com.daccaauto.pos.dto.vehicle.VehicleModelCreateRequest;
import com.daccaauto.pos.dto.vehicle.VehicleModelResponse;
import com.daccaauto.pos.dto.vehicle.VehicleModelUpdateRequest;
import com.daccaauto.pos.entity.VehicleMakeEntity;
import com.daccaauto.pos.entity.VehicleModelEntity;
import com.daccaauto.pos.exception.DuplicateResourceException;
import com.daccaauto.pos.exception.ResourceNotFoundException;
import com.daccaauto.pos.repository.VehicleMakeRepository;
import com.daccaauto.pos.repository.VehicleModelRepository;
import com.daccaauto.pos.service.VehicleModelService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class VehicleModelServiceImpl implements VehicleModelService {

    private final VehicleModelRepository vehicleModelRepository;
    private final VehicleMakeRepository vehicleMakeRepository;

    @Override
    public VehicleModelResponse create(VehicleModelCreateRequest request) {
        VehicleMakeEntity make = vehicleMakeRepository.findById(request.getMakeId())
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle make not found: " + request.getMakeId()));

        String name = request.getName().trim();

        if (vehicleModelRepository.existsByMakeIdAndNameIgnoreCase(request.getMakeId(), name)) {
            throw new DuplicateResourceException("Vehicle model already exists under this make: " + name);
        }

        VehicleModelEntity entity = new VehicleModelEntity();
        entity.setMake(make);
        entity.setName(name);
        entity.setActive(request.getActive() == null || request.getActive());

        return map(vehicleModelRepository.save(entity));
    }

    @Override
    public VehicleModelResponse update(Long id, VehicleModelUpdateRequest request) {
        VehicleModelEntity entity = vehicleModelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle model not found: " + id));

        VehicleMakeEntity make = vehicleMakeRepository.findById(request.getMakeId())
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle make not found: " + request.getMakeId()));

        String name = request.getName().trim();

        if (vehicleModelRepository.existsByMakeIdAndNameIgnoreCaseAndIdNot(request.getMakeId(), name, id)) {
            throw new DuplicateResourceException("Vehicle model already exists under this make: " + name);
        }

        entity.setMake(make);
        entity.setName(name);
        entity.setActive(request.getActive() == null || request.getActive());

        return map(vehicleModelRepository.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public VehicleModelResponse getById(Long id) {
        return map(
                vehicleModelRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Vehicle model not found: " + id))
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<VehicleModelResponse> getAll(Long makeId) {
        List<VehicleModelEntity> entities = makeId == null
                ? vehicleModelRepository.findAllByOrderByNameAsc()
                : vehicleModelRepository.findByMakeIdOrderByNameAsc(makeId);

        return entities.stream().map(this::map).toList();
    }

    @Override
    public void delete(Long id) {
        VehicleModelEntity entity = vehicleModelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle model not found: " + id));

        vehicleModelRepository.delete(entity);
    }

    private VehicleModelResponse map(VehicleModelEntity entity) {
        return new VehicleModelResponse(
                entity.getId(),
                entity.getMake().getId(),
                entity.getMake().getName(),
                entity.getName(),
                entity.isActive()
        );
    }
}