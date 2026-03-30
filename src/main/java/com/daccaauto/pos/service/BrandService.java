package com.daccaauto.pos.service;

import com.daccaauto.pos.dto.brand.*;

import java.util.List;

public interface BrandService {
    BrandResponse create(BrandCreateRequest request);
    BrandResponse update(Long id, BrandUpdateRequest request);
    BrandResponse getById(Long id);
    List<BrandResponse> getAll();
    void delete(Long id);
}