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
    public VehicleApplicationResponse create(VehicleApplicationCreateRequest request) {
        VehicleMakeEntity make = vehicleMakeRepository.findById(request.getVehicleMakeId())
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle make not found: " + request.getVehicleMakeId()));

        VehicleModelEntity model = vehicleModelRepository.findById(request.getVehicleModelId())
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle model not found: " + request.getVehicleModelId()));

        validateMakeModel(make, model);

        String variantLabel = trimToNull(request.getVariantLabel());

        if (vehicleApplicationRepository.existsDuplicate(
                request.getVehicleMakeId(),
                request.getVehicleModelId(),
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
    public VehicleApplicationResponse update(Long id, VehicleApplicationUpdateRequest request) {
        VehicleApplicationEntity entity = vehicleApplicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle application not found: " + id));

        VehicleMakeEntity make = vehicleMakeRepository.findById(request.getVehicleMakeId())
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle make not found: " + request.getVehicleMakeId()));

        VehicleModelEntity model = vehicleModelRepository.findById(request.getVehicleModelId())
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle model not found: " + request.getVehicleModelId()));

        validateMakeModel(make, model);

        String variantLabel = trimToNull(request.getVariantLabel());

        if (vehicleApplicationRepository.existsDuplicateExcludingId(
                request.getVehicleMakeId(),
                request.getVehicleModelId(),
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
    public void delete(Long id) {
        VehicleApplicationEntity entity = vehicleApplicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle application not found: " + id));

        vehicleApplicationRepository.delete(entity);
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
                entity.getDisplayName(),
                entity.isActive()
        );
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}