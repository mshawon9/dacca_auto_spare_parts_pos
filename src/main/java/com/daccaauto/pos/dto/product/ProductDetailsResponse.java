package com.daccaauto.pos.dto.product;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record ProductDetailsResponse(
    ProductResponse product,
    List<PartNumberGroup> partNumberGroups,
    int partNumberCount,
    List<ProductVariantSummary> variants,
    BigDecimal variantTotalStockQuantity,
    BigDecimal totalStockQuantity,
    List<StockSummary> stockSummaries,
    List<PriceHistorySummary> priceHistories
) {
    public record PartNumberGroup(
        Long productId,
        String productName,
        String partNumber,
        List<String> alternativePartNumbers,
        boolean currentProduct
    ) {
    }

    public record StockSummary(
        Long storeId,
        String storeName,
        BigDecimal quantity,
        BigDecimal sellingPrice
    ) {
    }

    public record PriceHistorySummary(
        String storeName,
        BigDecimal oldPrice,
        BigDecimal newPrice,
        String note,
        OffsetDateTime changedAt
    ) {
    }

    public record ProductVariantSummary(
        Long productId,
        String productName,
        String brandName,
        String partNumber,
        List<String> alternativePartNumbers,
        String position,
        String dimension,
        BigDecimal totalStockQuantity,
        boolean currentProduct
    ) {
    }
}
