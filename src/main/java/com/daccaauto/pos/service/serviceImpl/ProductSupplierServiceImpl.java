package com.daccaauto.pos.service.serviceImpl;

import com.daccaauto.pos.dto.supplier.ProductSupplierCreateRequest;
import com.daccaauto.pos.dto.supplier.ProductSupplierResponse;
import com.daccaauto.pos.dto.supplier.ProductSupplierUpdateRequest;
import com.daccaauto.pos.entity.ProductEntity;
import com.daccaauto.pos.entity.ProductSupplierEntity;
import com.daccaauto.pos.entity.SupplierEntity;
import com.daccaauto.pos.exception.DuplicateResourceException;
import com.daccaauto.pos.exception.ResourceNotFoundException;
import com.daccaauto.pos.repository.ProductRepository;
import com.daccaauto.pos.repository.ProductSupplierRepository;
import com.daccaauto.pos.repository.SupplierRepository;
import com.daccaauto.pos.service.ProductSupplierService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductSupplierServiceImpl implements ProductSupplierService {

    private final ProductSupplierRepository productSupplierRepository;
    private final ProductRepository productRepository;
    private final SupplierRepository supplierRepository;

    @Override
    public ProductSupplierResponse create(ProductSupplierCreateRequest request) {
        if (productSupplierRepository.existsByProductIdAndSupplierId(
            request.getProductId(), request.getSupplierId())) {
            throw new DuplicateResourceException("This supplier is already linked to this product");
        }

        ProductEntity product = productRepository.findById(request.getProductId())
            .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + request.getProductId()));

        SupplierEntity supplier = supplierRepository.findById(request.getSupplierId())
            .orElseThrow(() -> new ResourceNotFoundException("Supplier not found: " + request.getSupplierId()));

        ProductSupplierEntity entity = new ProductSupplierEntity();
        entity.setProduct(product);
        entity.setSupplier(supplier);
        entity.setSupplierProductCode(trimToNull(request.getSupplierProductCode()));
        entity.setPriceInput(request.getPriceInput().trim());
        entity.setPriceValue(parsePriceValue(request.getPriceInput()));
        entity.setCurrency(trimToNull(request.getCurrency()));
        entity.setNotes(trimToNull(request.getNotes()));
        entity.setActive(request.getActive() == null || request.getActive());

        return map(productSupplierRepository.save(entity));
    }

    @Override
    public ProductSupplierResponse update(Long id, ProductSupplierUpdateRequest request) {
        ProductSupplierEntity entity = productSupplierRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Product supplier not found: " + id));

        if (productSupplierRepository.existsByProductIdAndSupplierIdAndIdNot(
            request.getProductId(), request.getSupplierId(), id)) {
            throw new DuplicateResourceException("This supplier is already linked to this product");
        }

        ProductEntity product = productRepository.findById(request.getProductId())
            .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + request.getProductId()));

        SupplierEntity supplier = supplierRepository.findById(request.getSupplierId())
            .orElseThrow(() -> new ResourceNotFoundException("Supplier not found: " + request.getSupplierId()));

        entity.setProduct(product);
        entity.setSupplier(supplier);
        entity.setSupplierProductCode(trimToNull(request.getSupplierProductCode()));
        entity.setPriceInput(request.getPriceInput().trim());
        entity.setPriceValue(parsePriceValue(request.getPriceInput()));
        entity.setCurrency(trimToNull(request.getCurrency()));
        entity.setNotes(trimToNull(request.getNotes()));
        entity.setActive(request.getActive() == null || request.getActive());

        return map(productSupplierRepository.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public ProductSupplierResponse getById(Long id) {
        return map(
            productSupplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product supplier not found: " + id))
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductSupplierResponse> getByProductId(Long productId) {
        return productSupplierRepository.findByProductIdOrderBySupplierNameAsc(productId)
            .stream()
            .map(this::map)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductSupplierResponse> getBySupplierId(Long supplierId) {
        return productSupplierRepository.findBySupplierIdOrderByProductNameAsc(supplierId)
            .stream()
            .map(this::map)
            .toList();
    }

    @Override
    public void delete(Long id) {
        ProductSupplierEntity entity = productSupplierRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Product supplier not found: " + id));

        productSupplierRepository.delete(entity);
    }

    private ProductSupplierResponse map(ProductSupplierEntity entity) {
        return new ProductSupplierResponse(
            entity.getId(),
            entity.getProduct().getId(),
            entity.getProduct().getName(),
            entity.getProduct().getPartNumber(),
            entity.getSupplier().getId(),
            entity.getSupplier().getName(),
            entity.getSupplier().getTrnNumber(),
            entity.getSupplierProductCode(),
            entity.getPriceInput(),
            entity.getPriceValue(),
            entity.getCurrency(),
            entity.getNotes(),
            entity.isActive()
        );
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private BigDecimal parsePriceValue(String input) {
        if (input == null || input.isBlank()) {
            return null;
        }

        String normalized = input.trim().replace(",", "");

        if (!normalized.matches("^\\d+(\\.\\d+)?$")) {
            return null;
        }

        try {
            return new BigDecimal(normalized);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}