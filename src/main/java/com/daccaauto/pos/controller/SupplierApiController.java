package com.daccaauto.pos.controller;

import com.daccaauto.pos.dto.supplier.SupplierCreateRequest;
import com.daccaauto.pos.dto.supplier.SupplierResponse;
import com.daccaauto.pos.dto.supplier.SupplierUpdateRequest;
import com.daccaauto.pos.service.SupplierService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/suppliers")
@RequiredArgsConstructor
public class SupplierApiController {

    private final SupplierService supplierService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SupplierResponse create(@Valid @RequestBody SupplierCreateRequest request) {
        return supplierService.create(request);
    }

    @PutMapping("/{id}")
    public SupplierResponse update(@PathVariable Long id,
                                   @Valid @RequestBody SupplierUpdateRequest request) {
        return supplierService.update(id, request);
    }

    @GetMapping("/{id}")
    public SupplierResponse getById(@PathVariable Long id) {
        return supplierService.getById(id);
    }

    @GetMapping
    public List<SupplierResponse> getAll() {
        return supplierService.getAll();
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        supplierService.delete(id);
    }
}