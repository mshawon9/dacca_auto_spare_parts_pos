package com.daccaauto.pos.controller;

import com.daccaauto.pos.dto.supplier.ProductSupplierCreateRequest;
import com.daccaauto.pos.dto.supplier.ProductSupplierResponse;
import com.daccaauto.pos.dto.supplier.ProductSupplierUpdateRequest;
import com.daccaauto.pos.service.ProductSupplierService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/product-suppliers")
@RequiredArgsConstructor
public class ProductSupplierApiController {

    private final ProductSupplierService productSupplierService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductSupplierResponse create(@Valid @RequestBody ProductSupplierCreateRequest request) {
        return productSupplierService.create(request);
    }

    @PutMapping("/{id}")
    public ProductSupplierResponse update(@PathVariable Long id,
                                          @Valid @RequestBody ProductSupplierUpdateRequest request) {
        return productSupplierService.update(id, request);
    }

    @GetMapping("/{id}")
    public ProductSupplierResponse getById(@PathVariable Long id) {
        return productSupplierService.getById(id);
    }

    @GetMapping("/by-product/{productId}")
    public List<ProductSupplierResponse> getByProductId(@PathVariable Long productId) {
        return productSupplierService.getByProductId(productId);
    }

    @GetMapping("/by-supplier/{supplierId}")
    public List<ProductSupplierResponse> getBySupplierId(@PathVariable Long supplierId) {
        return productSupplierService.getBySupplierId(supplierId);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        productSupplierService.delete(id);
    }
}