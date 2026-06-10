package com.daccaauto.pos.service.serviceImpl;

import  com.daccaauto.pos.dto.category.ProductCategoryCreateRequest;
import  com.daccaauto.pos.dto.category.ProductCategoryResponse;
import  com.daccaauto.pos.dto.category.ProductCategoryUpdateRequest;
import  com.daccaauto.pos.entity.BrandCategoryEntity;
import  com.daccaauto.pos.entity.BrandEntity;
import  com.daccaauto.pos.entity.ProductCategoryEntity;
import  com.daccaauto.pos.exception.DuplicateResourceException;
import  com.daccaauto.pos.exception.ResourceNotFoundException;
import  com.daccaauto.pos.repository.BrandCategoryRepository;
import  com.daccaauto.pos.repository.BrandRepository;
import  com.daccaauto.pos.repository.ProductCategoryRepository;
import  com.daccaauto.pos.service.ProductCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductCategoryServiceImpl implements ProductCategoryService {

    private final ProductCategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
    private final BrandCategoryRepository brandCategoryRepository;

    @Override
    public ProductCategoryResponse create(ProductCategoryCreateRequest request) {
        String name = request.getName().trim();

        if (categoryRepository.existsByNameIgnoreCase(name)) {
            throw new DuplicateResourceException("Category already exists: " + name);
        }

        ProductCategoryEntity entity = new ProductCategoryEntity();
        entity.setName(name);
        entity.setDescription(trimToNull(request.getDescription()));
        entity.setActive(request.getActive() == null || request.getActive());

        ProductCategoryEntity saved = categoryRepository.save(entity);

        syncCategoryBrands(saved, request.getBrandIds());

        return map(saved);
    }

    @Override
    public ProductCategoryResponse update(Long id, ProductCategoryUpdateRequest request) {
        ProductCategoryEntity entity = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + id));

        String name = request.getName().trim();

        if (categoryRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new DuplicateResourceException("Category already exists: " + name);
        }

        entity.setName(name);
        entity.setDescription(trimToNull(request.getDescription()));
        entity.setActive(request.getActive() == null || request.getActive());

        ProductCategoryEntity saved = categoryRepository.save(entity);

        syncCategoryBrands(saved, request.getBrandIds());

        return map(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductCategoryResponse getById(Long id) {
        ProductCategoryEntity entity = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + id));

        return map(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductCategoryResponse> getAll() {
        return categoryRepository.findAllByOrderByNameAsc()
                .stream()
                .map(this::map)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductCategoryResponse> getAllFilteredByBrand(Long brandId) {
        if (brandId == null) {
            return getAll();
        }

        return brandCategoryRepository.findByBrandIdAndActiveTrueOrderByCategoryNameAsc(brandId)
                .stream()
                .map(BrandCategoryEntity::getCategory)
                .filter(ProductCategoryEntity::isActive)
                .distinct()
                .map(this::map)
                .toList();
    }

    @Override
    public void delete(Long id) {
        ProductCategoryEntity entity = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + id));

        brandCategoryRepository.deleteByCategory_Id(id);
        categoryRepository.delete(entity);
    }

    @Transactional
    public void syncCategoryBrands(ProductCategoryEntity category, Set<Long> brandIds) {
        brandCategoryRepository.deleteByCategory_Id(category.getId());
        brandCategoryRepository.flush();

        if (brandIds == null || brandIds.isEmpty()) {
            return;
        }

        Set<Long> uniqueBrandIds = new LinkedHashSet<>(brandIds);
        List<BrandEntity> brands = brandRepository.findAllById(uniqueBrandIds);

        Map<Long, BrandEntity> brandMap = brands.stream()
                .collect(Collectors.toMap(BrandEntity::getId, Function.identity()));

        List<BrandCategoryEntity> mappings = new ArrayList<>();
        int order = 1;

        for (Long brandId : uniqueBrandIds) {
            BrandEntity brand = brandMap.get(brandId);
            if (brand == null) {
                throw new IllegalArgumentException("Brand not found: " + brandId);
            }

            BrandCategoryEntity mapping = new BrandCategoryEntity();
            mapping.setBrand(brand);
            mapping.setCategory(category);
            mapping.setDisplayOrder(order++);
            mapping.setActive(true);

            mappings.add(mapping);
        }

        brandCategoryRepository.saveAll(mappings);
    }

    private ProductCategoryResponse map(ProductCategoryEntity entity) {
        return new ProductCategoryResponse(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.isActive()
        );
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}