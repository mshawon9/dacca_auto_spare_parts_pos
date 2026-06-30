package com.daccaauto.pos.service.serviceImpl;

import com.daccaauto.pos.dto.vehicle.VehicleApplicationCreateRequest;
import com.daccaauto.pos.dto.vehicle.VehicleApplicationResponse;
import com.daccaauto.pos.dto.vehicle.VehicleApplicationUpdateRequest;
import com.daccaauto.pos.entity.VehicleApplicationEntity;
import com.daccaauto.pos.entity.VehicleMakeEntity;
import com.daccaauto.pos.entity.VehicleModelEntity;
import com.daccaauto.pos.exception.DuplicateResourceException;
import com.daccaauto.pos.exception.ResourceNotFoundException;
import com.daccaauto.pos.repository.VehicleApplicationRepository;
import com.daccaauto.pos.repository.VehicleMakeRepository;
import com.daccaauto.pos.repository.VehicleModelRepository;
import com.daccaauto.pos.service.VehicleApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional
public class VehicleApplicationServiceImpl implements VehicleApplicationService {

    private final VehicleApplicationRepository vehicleApplicationRepository;
    private final VehicleMakeRepository vehicleMakeRepository;
    private final VehicleModelRepository vehicleModelRepository;

    @Override
    @CacheEvict(cacheNames = {"vehicleApplications", "vehicleMakes", "vehicleModels"}, allEntries = true)
    public VehicleApplicationResponse create(VehicleApplicationCreateRequest request) {
        VehicleMakeEntity make = resolveMake(request.getVehicleMakeId(), request.getVehicleMakeName());
        VehicleModelEntity model = resolveModel(make, request.getVehicleModelId(), request.getVehicleModelName());

        String variantLabel = trimToNull(request.getVariantLabel());

        if (vehicleApplicationRepository.existsDuplicate(
                make.getId(),
                model.getId(),
                variantLabel,
                request.getYearFrom(),
                request.getYearTo()
        )) {
            throw new DuplicateResourceException("Vehicle application already exists");
        }

        VehicleApplicationEntity entity = new VehicleApplicationEntity();
        entity.setVehicleMake(make);
        entity.setVehicleModel(model);
        entity.setVariantLabel(variantLabel);
        entity.setYearFrom(request.getYearFrom());
        entity.setYearTo(request.getYearTo());
        entity.setActive(request.getActive() == null || request.getActive());

        return map(vehicleApplicationRepository.save(entity));
    }

    @Override
    @CacheEvict(cacheNames = {"vehicleApplications", "vehicleMakes", "vehicleModels"}, allEntries = true)
    public VehicleApplicationResponse update(Long id, VehicleApplicationUpdateRequest request) {
        VehicleApplicationEntity entity = vehicleApplicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle application not found: " + id));

        VehicleMakeEntity make = resolveMake(request.getVehicleMakeId(), request.getVehicleMakeName());
        VehicleModelEntity model = resolveModel(make, request.getVehicleModelId(), request.getVehicleModelName());

        String variantLabel = trimToNull(request.getVariantLabel());

        if (vehicleApplicationRepository.existsDuplicateExcludingId(
                make.getId(),
                model.getId(),
                variantLabel,
                request.getYearFrom(),
                request.getYearTo(),
                id
        )) {
            throw new DuplicateResourceException("Vehicle application already exists");
        }

        entity.setVehicleMake(make);
        entity.setVehicleModel(model);
        entity.setVariantLabel(variantLabel);
        entity.setYearFrom(request.getYearFrom());
        entity.setYearTo(request.getYearTo());
        entity.setActive(request.getActive() == null || request.getActive());

        return map(vehicleApplicationRepository.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public VehicleApplicationResponse getById(Long id) {
        return map(
                vehicleApplicationRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Vehicle application not found: " + id))
        );
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "vehicleApplications", key = "{#makeId, #modelId, #keyword}")
    public List<VehicleApplicationResponse> getAll(Long makeId, Long modelId, String keyword) {
        List<VehicleApplicationEntity> entities;

        if (makeId != null && modelId != null) {
            entities = vehicleApplicationRepository.findByVehicleMakeIdAndVehicleModelIdOrderByDisplayNameAsc(makeId, modelId);
        } else if (makeId != null) {
            entities = vehicleApplicationRepository.findByVehicleMakeIdOrderByDisplayNameAsc(makeId);
        } else {
            entities = vehicleApplicationRepository.findAllByOrderByDisplayNameAsc();
        }

        String q = keyword == null ? null : keyword.trim().toLowerCase(Locale.ROOT);

        return entities.stream()
                .filter(e -> q == null || q.isBlank()
                        || e.getDisplayName().toLowerCase(Locale.ROOT).contains(q)
                        || e.getVehicleMake().getName().toLowerCase(Locale.ROOT).contains(q)
                        || e.getVehicleModel().getName().toLowerCase(Locale.ROOT).contains(q)
                        || (e.getVariantLabel() != null && e.getVariantLabel().toLowerCase(Locale.ROOT).contains(q)))
                .map(this::map)
                .toList();
    }

    @Override
    @CacheEvict(cacheNames = "vehicleApplications", allEntries = true)
    public void delete(Long id) {
        VehicleApplicationEntity entity = vehicleApplicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle application not found: " + id));

        vehicleApplicationRepository.delete(entity);
    }

    private VehicleMakeEntity resolveMake(Long makeId, String makeName) {
        if (makeId != null) {
            return vehicleMakeRepository.findById(makeId)
                    .orElseThrow(() -> new ResourceNotFoundException("Vehicle make not found: " + makeId));
        }

        String name = trimToNull(makeName);
        if (name == null) {
            throw new ResourceNotFoundException("Vehicle make is required");
        }

        return vehicleMakeRepository.findByNameIgnoreCase(name)
                .orElseGet(() -> {
                    VehicleMakeEntity make = new VehicleMakeEntity();
                    make.setName(name);
                    make.setActive(true);
                    return vehicleMakeRepository.save(make);
                });
    }

    private VehicleModelEntity resolveModel(VehicleMakeEntity make, Long modelId, String modelName) {
        if (modelId != null) {
            VehicleModelEntity model = vehicleModelRepository.findById(modelId)
                    .orElseThrow(() -> new ResourceNotFoundException("Vehicle model not found: " + modelId));
            validateMakeModel(make, model);
            return model;
        }

        String name = trimToNull(modelName);
        if (name == null) {
            throw new ResourceNotFoundException("Vehicle model is required");
        }

        return vehicleModelRepository.findByMakeIdAndNameIgnoreCase(make.getId(), name)
                .orElseGet(() -> {
                    VehicleModelEntity model = new VehicleModelEntity();
                    model.setMake(make);
                    model.setName(name);
                    model.setActive(true);
                    return vehicleModelRepository.save(model);
                });
    }

    private void validateMakeModel(VehicleMakeEntity make, VehicleModelEntity model) {
        if (!model.getMake().getId().equals(make.getId())) {
            throw new DuplicateResourceException("Selected model does not belong to selected make");
        }
    }

    private VehicleApplicationResponse map(VehicleApplicationEntity entity) {
        return new VehicleApplicationResponse(
                entity.getId(),
                entity.getVehicleMake().getId(),
                entity.getVehicleMake().getName(),
                entity.getVehicleModel().getId(),
                entity.getVehicleModel().getName(),
                entity.getVariantLabel(),
                entity.getYearFrom(),
                entity.getYearTo(),
                entity.getVehicleMake().getName() + " " + entity.getDisplayName(),
                entity.isActive()
        );
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
