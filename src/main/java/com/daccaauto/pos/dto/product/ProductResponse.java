package com.daccaauto.pos.dto.product;

import java.util.List;
import java.util.Set;

public record ProductResponse(
    Long id,
    String name,
    String specLabel,
    String dimension,
    String sku,
    String partNumber,
    String barcode,
    String description,
    boolean hasImage,
    Long categoryId,
    String categoryName,
    Long brandId,
    String brandName,
    Set<Long> applicationIds,
    List<String> applicationDisplayNames,
    String applicationSummary,
    Long similarProductId,
    List<SimilarProductSummary> similarProducts,
    boolean active
) {
    public record SimilarProductSummary(Long id, String displayName) {
    }
}
