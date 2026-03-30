package com.daccaauto.pos.service;

import com.daccaauto.pos.dto.supplier.*;

import java.util.List;

public interface ProductSupplierService {

    ProductSupplierResponse create(ProductSupplierCreateRequest request);

    ProductSupplierResponse update(Long id, ProductSupplierUpdateRequest request);

    ProductSupplierResponse getById(Long id);

    List<ProductSupplierResponse> getByProductId(Long productId);

    List<ProductSupplierResponse> getBySupplierId(Long supplierId);

    void delete(Long id);
}