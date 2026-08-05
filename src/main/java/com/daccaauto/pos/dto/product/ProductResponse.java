package com.daccaauto.pos.dto.product;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

public record ProductResponse(
    Long id,
    String name,
    String specLabel,
    String position,
    String dimension,
    String sku,
    BigDecimal reorderLevel,
    String partNumber,
    String alternativePartNumber,
    List<String> alternativePartNumbers,
    String barcode,
    String description,
    boolean hasImage,
    Long categoryId,
    String categoryName,
    Long brandId,
    String brandName,
    Long productGroupId,
    String productGroupName,
    Set<Long> applicationIds,
    List<String> applicationDisplayNames,
    String applicationSummary,
    String applicationMakeSummary,
    BigDecimal totalStockQuantity,
    BigDecimal lastCostPrice,
    Long similarProductId,
    List<SimilarProductSummary> similarProducts,
    boolean active
) {
    public record SimilarProductSummary(Long id, String displayName) {
    }
}
