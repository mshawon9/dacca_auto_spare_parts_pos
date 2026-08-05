package com.daccaauto.pos.service;

import com.daccaauto.pos.dto.product.ProductCreateRequest;
import com.daccaauto.pos.dto.product.ProductDetailsResponse;
import com.daccaauto.pos.dto.product.ProductResponse;
import com.daccaauto.pos.dto.product.ProductUpdateRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface ProductService {

    ProductResponse create(ProductCreateRequest request);

    ProductResponse update(Long id, ProductUpdateRequest request);

    ProductResponse getById(Long id);

    ProductDetailsResponse getDetails(Long id);

    Optional<ProductResponse> getLastCreatedProduct();

    List<ProductResponse.SimilarProductSummary> getSimilarityGroup(Long productId);

    ProductImage getImage(Long productId);

    String suggestBarcode(Long categoryId);

    List<ProductResponse> search(String keyword,
                                 Long categoryId,
                                 Long brandId,
                                 Long applicationId,
                                 Long makeId,
                                 Boolean active);

    Page<ProductResponse> searchPage(String keyword,
                                     Long categoryId,
                                     Long brandId,
                                     Long applicationId,
                                     Long makeId,
                                     Boolean active,
                                     Pageable pageable);

    void delete(Long id);

    record ProductImage(byte[] content, String contentType) {
    }
}
