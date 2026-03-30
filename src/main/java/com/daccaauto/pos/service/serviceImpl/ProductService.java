package com.daccaauto.pos.service.serviceImpl;

import com.daccaauto.pos.dto.CreateProductRequest;
import com.daccaauto.pos.entity.*;
import com.daccaauto.pos.repository.*;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final BrandRepository brandRepository;
    private final ProductCategoryRepository categoryRepository;
    private final VehicleApplicationRepository vehicleApplicationRepository;
    private final BrandCategoryRepository brandCategoryRepository;

    @Transactional
    public ProductEntity create(CreateProductRequest request) {
        String normalizedPartNumber = normalizePartNumber(request.getPartNumber());

        if (productRepository.existsByBrandIdAndNormalizedPartNumber(request.getBrandId(), normalizedPartNumber)) {
            throw new IllegalArgumentException("Same brand cannot have duplicate part number");
        }

        if (!brandCategoryRepository.existsByBrandIdAndCategoryIdAndActiveTrue(
                request.getBrandId(), request.getCategoryId())) {
            throw new IllegalArgumentException("Selected brand is not allowed for the selected category");
        }

        BrandEntity brand = brandRepository.findById(request.getBrandId())
                .orElseThrow(() -> new EntityNotFoundException("Brand not found"));

        ProductCategoryEntity category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new EntityNotFoundException("Category not found"));

        ProductEntity product = new ProductEntity();
        product.setName(request.getName().trim());
        product.setSpecLabel(trimToNull(request.getSpecLabel()));
        product.setSku(trimToNull(request.getSku()));
        product.setPartNumber(request.getPartNumber().trim());
        product.setBarcode(trimToNull(request.getBarcode()));
        product.setDescription(trimToNull(request.getDescription()));
        product.setBrand(brand);
        product.setCategory(category);

        if (request.getApplicationIds() != null) {
            for (Long applicationId : request.getApplicationIds()) {
                VehicleApplicationEntity vehicleApplication = vehicleApplicationRepository.findById(applicationId)
                        .orElseThrow(() -> new EntityNotFoundException("Vehicle application not found: " + applicationId));

                ProductApplicationEntity join = new ProductApplicationEntity();
                join.setVehicleApplication(vehicleApplication);
                product.addApplication(join);
            }
        }

        return productRepository.save(product);
    }

    private String normalizePartNumber(String input) {
        return input == null ? null
                : input.replaceAll("[\\s\\-_/.]", "").toUpperCase(Locale.ROOT);
    }

    private String trimToNull(String input) {
        return input == null || input.isBlank() ? null : input.trim();
    }
}