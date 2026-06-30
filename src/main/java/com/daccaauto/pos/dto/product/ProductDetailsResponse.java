package com.daccaauto.pos.dto.product;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record ProductDetailsResponse(
    ProductResponse product,
    BigDecimal totalStockQuantity,
    List<StockSummary> stockSummaries,
    List<PriceHistorySummary> priceHistories
) {
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
}
