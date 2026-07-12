package com.daccaauto.pos.service;


import com.daccaauto.pos.dto.supplier.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface SupplierService {

    SupplierResponse create(SupplierCreateRequest request);

    SupplierResponse update(Long id, SupplierUpdateRequest request);

    SupplierResponse getById(Long id);

    List<SupplierResponse> getAll();

    Page<SupplierResponse> getPage(Pageable pageable);

    void delete(Long id);
}
