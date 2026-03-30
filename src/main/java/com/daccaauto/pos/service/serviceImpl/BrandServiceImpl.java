package com.daccaauto.pos.service.serviceImpl;

import com.daccaauto.pos.dto.brand.BrandCreateRequest;
import com.daccaauto.pos.dto.brand.BrandResponse;
import com.daccaauto.pos.dto.brand.BrandUpdateRequest;
import com.daccaauto.pos.entity.BrandEntity;
import com.daccaauto.pos.exception.DuplicateResourceException;
import com.daccaauto.pos.exception.ResourceNotFoundException;
import com.daccaauto.pos.repository.BrandRepository;
import com.daccaauto.pos.service.BrandService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class BrandServiceImpl implements BrandService {

    private final BrandRepository brandRepository;

    @Override
    public BrandResponse create(BrandCreateRequest request) {
        if (brandRepository.existsByNameIgnoreCase(request.getName().trim())) {
            throw new DuplicateResourceException("Brand already exists: " + request.getName());
        }

        BrandEntity entity = new BrandEntity();
        entity.setName(request.getName().trim());
        entity.setCode(trimToNull(request.getCode()));
        entity.setActive(request.getActive() == null || request.getActive());

        return map(brandRepository.save(entity));
    }

    @Override
    public BrandResponse update(Long id, BrandUpdateRequest request) {
        BrandEntity entity = brandRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Brand not found: " + id));

        if (brandRepository.existsByNameIgnoreCaseAndIdNot(request.getName().trim(), id)) {
            throw new DuplicateResourceException("Brand already exists: " + request.getName());
        }

        entity.setName(request.getName().trim());
        entity.setCode(trimToNull(request.getCode()));
        entity.setActive(request.getActive() == null || request.getActive());

        return map(brandRepository.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public BrandResponse getById(Long id) {
        return map(brandRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Brand not found: " + id)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<BrandResponse> getAll() {
        return brandRepository.findAll().stream().map(this::map).toList();
    }

    @Override
    public void delete(Long id) {
        BrandEntity entity = brandRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Brand not found: " + id));
        brandRepository.delete(entity);
    }

    private BrandResponse map(BrandEntity entity) {
        return new BrandResponse(
                entity.getId(),
                entity.getName(),
                entity.getCode(),
                entity.isActive()
        );
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}