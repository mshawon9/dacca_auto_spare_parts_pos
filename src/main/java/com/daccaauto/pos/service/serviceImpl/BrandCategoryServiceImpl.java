package com.daccaauto.pos.service.serviceImpl;

import com.daccaauto.pos.dto.brand.BrandCategoryCreateRequest;
import com.daccaauto.pos.dto.brand.BrandCategoryResponse;
import com.daccaauto.pos.dto.brand.BrandCategoryUpdateRequest;
import com.daccaauto.pos.dto.brand.BrandResponse;
import com.daccaauto.pos.entity.BrandCategoryEntity;
import com.daccaauto.pos.entity.BrandEntity;
import com.daccaauto.pos.entity.ProductCategoryEntity;
import com.daccaauto.pos.exception.DuplicateResourceException;
import com.daccaauto.pos.exception.ResourceNotFoundException;
import com.daccaauto.pos.repository.BrandCategoryRepository;
import com.daccaauto.pos.repository.BrandRepository;
import com.daccaauto.pos.repository.ProductCategoryRepository;
import com.daccaauto.pos.service.BrandCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class BrandCategoryServiceImpl implements BrandCategoryService {

    private final BrandCategoryRepository brandCategoryRepository;
    private final BrandRepository brandRepository;
    private final ProductCategoryRepository categoryRepository;

    @Override
    public BrandCategoryResponse create(BrandCategoryCreateRequest request) {
        if (brandCategoryRepository.existsByBrandIdAndCategoryId(request.getBrandId(), request.getCategoryId())) {
            throw new DuplicateResourceException("Brand is already mapped to this category");
        }

        BrandEntity brand = brandRepository.findById(request.getBrandId())
            .orElseThrow(() -> new ResourceNotFoundException("Brand not found: " + request.getBrandId()));

        ProductCategoryEntity category = categoryRepository.findById(request.getCategoryId())
            .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + request.getCategoryId()));

        BrandCategoryEntity entity = new BrandCategoryEntity();
        entity.setBrand(brand);
        entity.setCategory(category);
        entity.setDisplayOrder(request.getDisplayOrder());
        entity.setActive(request.getActive() == null || request.getActive());

        return map(brandCategoryRepository.save(entity));
    }

    @Override
    public BrandCategoryResponse update(Long id, BrandCategoryUpdateRequest request) {
        BrandCategoryEntity entity = brandCategoryRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Brand category mapping not found: " + id));

        if (brandCategoryRepository.existsByBrandIdAndCategoryIdAndIdNot(
            request.getBrandId(), request.getCategoryId(), id)) {
            throw new DuplicateResourceException("Brand is already mapped to this category");
        }

        BrandEntity brand = brandRepository.findById(request.getBrandId())
            .orElseThrow(() -> new ResourceNotFoundException("Brand not found: " + request.getBrandId()));

        ProductCategoryEntity category = categoryRepository.findById(request.getCategoryId())
            .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + request.getCategoryId()));

        entity.setBrand(brand);
        entity.setCategory(category);
        entity.setDisplayOrder(request.getDisplayOrder());
        entity.setActive(request.getActive() == null || request.getActive());

        return map(brandCategoryRepository.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public BrandCategoryResponse getById(Long id) {
        return map(
            brandCategoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Brand category mapping not found: " + id))
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<BrandCategoryResponse> getAll() {
        return brandCategoryRepository.findAll()
            .stream()
            .map(this::map)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<BrandResponse> getBrandsByCategoryId(Long categoryId) {
        return brandCategoryRepository.findByCategoryIdAndActiveTrueOrderByDisplayOrderAscBrandNameAsc(categoryId)
            .stream()
            .map(BrandCategoryEntity::getBrand)
            .filter(BrandEntity::isActive)
            .sorted(Comparator.comparing(BrandEntity::getName, String.CASE_INSENSITIVE_ORDER))
            .map(this::mapBrand)
            .toList();
    }

    @Override
    public void delete(Long id) {
        BrandCategoryEntity entity = brandCategoryRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Brand category mapping not found: " + id));

        brandCategoryRepository.delete(entity);
    }

    private BrandCategoryResponse map(BrandCategoryEntity entity) {
        return new BrandCategoryResponse(
            entity.getId(),
            entity.getBrand().getId(),
            entity.getBrand().getName(),
            entity.getCategory().getId(),
            entity.getCategory().getName(),
            entity.getDisplayOrder(),
            entity.isActive()
        );
    }

    private BrandResponse mapBrand(BrandEntity entity) {
        return new BrandResponse(
            entity.getId(),
            entity.getName(),
            entity.getCode(),
            entity.isActive()
        );
    }
}