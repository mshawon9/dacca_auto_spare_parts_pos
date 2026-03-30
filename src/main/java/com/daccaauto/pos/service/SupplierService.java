package com.daccaauto.pos.service;


import com.daccaauto.pos.dto.supplier.*;

import java.util.List;

public interface SupplierService {

    SupplierResponse create(SupplierCreateRequest request);

    SupplierResponse update(Long id, SupplierUpdateRequest request);

    SupplierResponse getById(Long id);

    List<SupplierResponse> getAll();

    void delete(Long id);
}