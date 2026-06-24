package com.daccaauto.pos.service;

import com.daccaauto.pos.dto.product.ProductCreateRequest;
import com.daccaauto.pos.dto.product.ProductResponse;
import com.daccaauto.pos.dto.product.ProductUpdateRequest;

import java.util.List;

public interface ProductService {

    ProductResponse create(ProductCreateRequest request);

    ProductResponse update(Long id, ProductUpdateRequest request);

    ProductResponse getById(Long id);

    List<ProductResponse.SimilarProductSummary> getSimilarityGroup(Long productId);

    ProductImage getImage(Long productId);

    List<ProductResponse> search(String keyword,
                                 Long categoryId,
                                 Long brandId,
                                 Long applicationId,
                                 Boolean active);

    void delete(Long id);

    record ProductImage(byte[] content, String contentType) {
    }
}
