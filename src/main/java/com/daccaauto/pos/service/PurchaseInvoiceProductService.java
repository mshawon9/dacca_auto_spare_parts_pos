package com.daccaauto.pos.service;

import com.daccaauto.pos.dto.brand.BrandCategoryCreateRequest;
import com.daccaauto.pos.dto.brand.BrandCategoryUpdateRequest;
import com.daccaauto.pos.dto.product.ProductCreateRequest;
import com.daccaauto.pos.dto.product.ProductResponse;
import com.daccaauto.pos.repository.BrandCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PurchaseInvoiceProductService {
    private final ProductService products;
    private final BrandCategoryRepository mappings;
    private final BrandCategoryService brands;

    @Transactional
    public ProductResponse create(ProductCreateRequest request) {
        if (!mappings.existsByBrandIdAndCategoryIdAndActiveTrue(request.getBrandId(), request.getCategoryId())) {
            var existing = mappings.findByCategory_Id(request.getCategoryId()).stream()
                .filter(m -> m.getBrand().getId().equals(request.getBrandId())).findFirst();
            if (existing.isPresent()) {
                var update = new BrandCategoryUpdateRequest();
                update.setBrandId(request.getBrandId());
                update.setCategoryId(request.getCategoryId());
                update.setDisplayOrder(existing.get().getDisplayOrder());
                update.setActive(true);
                brands.update(existing.get().getId(), update);
            } else {
                var mapping = new BrandCategoryCreateRequest();
                mapping.setBrandId(request.getBrandId());
                mapping.setCategoryId(request.getCategoryId());
                brands.create(mapping);
            }
        }
        return products.create(request);
    }
}
