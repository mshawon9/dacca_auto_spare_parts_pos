package com.daccaauto.pos.service;

import com.daccaauto.pos.dto.product.ProductCreateRequest;
import com.daccaauto.pos.dto.product.ProductResponse;
import com.daccaauto.pos.dto.product.ProductUpdateRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ProductService {

    ProductResponse create(ProductCreateRequest request);

    ProductResponse update(Long id, ProductUpdateRequest request);

    ProductResponse getById(Long id);

    List<ProductResponse.SimilarProductSummary> getSimilarityGroup(Long productId);

    ProductImage getImage(Long productId);

    String suggestBarcode(Long categoryId);

    List<ProductResponse> search(String keyword,
                                 Long categoryId,
                                 Long brandId,
                                 Long applicationId,
                                 Boolean active);

    Page<ProductResponse> searchPage(String keyword,
                                     Long categoryId,
                                     Long brandId,
                                     Long applicationId,
                                     Boolean active,
                                     Pageable pageable);

    void delete(Long id);

    record ProductImage(byte[] content, String contentType) {
    }
}
