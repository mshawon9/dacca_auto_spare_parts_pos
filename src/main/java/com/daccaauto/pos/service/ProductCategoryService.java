package com.daccaauto.pos.service;

import com.daccaauto.pos.dto.category.*;

import java.util.List;

public interface ProductCategoryService {
    ProductCategoryResponse create(ProductCategoryCreateRequest request);
    ProductCategoryResponse update(Long id, ProductCategoryUpdateRequest request);
    ProductCategoryResponse getById(Long id);
    List<ProductCategoryResponse> getAll();
    void delete(Long id);

    List<ProductCategoryResponse> getAllFilteredByBrand(Long brandId);
}