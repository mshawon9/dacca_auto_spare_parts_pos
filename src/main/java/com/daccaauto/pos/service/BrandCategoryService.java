package com.daccaauto.pos.service;

import com.daccaauto.pos.dto.brand.*;

import java.util.List;

public interface BrandCategoryService {

    BrandCategoryResponse create(BrandCategoryCreateRequest request);

    BrandCategoryResponse update(Long id, BrandCategoryUpdateRequest request);

    BrandCategoryResponse getById(Long id);

    List<BrandCategoryResponse> getAll();

    List<BrandResponse> getBrandsByCategoryId(Long categoryId);

    void delete(Long id);
}