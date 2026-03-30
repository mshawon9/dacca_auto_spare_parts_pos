package com.daccaauto.pos.service.serviceImpl;

import com.daccaauto.pos.dto.supplier.SupplierCreateRequest;
import com.daccaauto.pos.dto.supplier.SupplierResponse;
import com.daccaauto.pos.dto.supplier.SupplierUpdateRequest;
import com.daccaauto.pos.entity.SupplierEntity;
import com.daccaauto.pos.exception.DuplicateResourceException;
import com.daccaauto.pos.exception.ResourceNotFoundException;
import com.daccaauto.pos.repository.SupplierRepository;
import com.daccaauto.pos.service.SupplierService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class SupplierServiceImpl implements SupplierService {

    private final SupplierRepository supplierRepository;

    @Override
    public SupplierResponse create(SupplierCreateRequest request) {
        String name = request.getName().trim();
        String trnNumber = trimToNull(request.getTrnNumber());

        if (supplierRepository.existsByNameIgnoreCase(name)) {
            throw new DuplicateResourceException("Supplier already exists: " + name);
        }

        if (trnNumber != null && supplierRepository.existsByTrnNumberIgnoreCase(trnNumber)) {
            throw new DuplicateResourceException("TRN number already exists: " + trnNumber);
        }

        SupplierEntity entity = new SupplierEntity();
        entity.setName(name);
        entity.setContactPerson(trimToNull(request.getContactPerson()));
        entity.setPhone(trimToNull(request.getPhone()));
        entity.setEmail(trimToNull(request.getEmail()));
        entity.setAddress(trimToNull(request.getAddress()));
        entity.setTrnNumber(trnNumber);
        entity.setActive(request.getActive() == null || request.getActive());

        return map(supplierRepository.save(entity));
    }

    @Override
    public SupplierResponse update(Long id, SupplierUpdateRequest request) {
        SupplierEntity entity = supplierRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Supplier not found: " + id));

        String name = request.getName().trim();
        String trnNumber = trimToNull(request.getTrnNumber());

        if (supplierRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new DuplicateResourceException("Supplier already exists: " + name);
        }

        if (trnNumber != null && supplierRepository.existsByTrnNumberIgnoreCaseAndIdNot(trnNumber, id)) {
            throw new DuplicateResourceException("TRN number already exists: " + trnNumber);
        }

        entity.setName(name);
        entity.setContactPerson(trimToNull(request.getContactPerson()));
        entity.setPhone(trimToNull(request.getPhone()));
        entity.setEmail(trimToNull(request.getEmail()));
        entity.setAddress(trimToNull(request.getAddress()));
        entity.setTrnNumber(trnNumber);
        entity.setActive(request.getActive() == null || request.getActive());

        return map(supplierRepository.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public SupplierResponse getById(Long id) {
        return map(
            supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found: " + id))
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<SupplierResponse> getAll() {
        return supplierRepository.findAllByOrderByNameAsc()
            .stream()
            .map(this::map)
            .toList();
    }

    @Override
    public void delete(Long id) {
        SupplierEntity entity = supplierRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Supplier not found: " + id));

        supplierRepository.delete(entity);
    }

    private SupplierResponse map(SupplierEntity entity) {
        return new SupplierResponse(
            entity.getId(),
            entity.getName(),
            entity.getContactPerson(),
            entity.getPhone(),
            entity.getEmail(),
            entity.getAddress(),
            entity.getTrnNumber(),
            entity.isActive()
        );
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}