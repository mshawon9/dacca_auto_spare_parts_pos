package com.daccaauto.pos.dto.dashboard;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record DashboardSummaryResponse(
    long totalProducts,
    long activeProducts,
    long stores,
    long categories,
    long brands,
    long vehicleApplications,
    BigDecimal totalStockQuantity,
    long zeroStockRecords,
    long productsWithoutPrice,
    List<RecentStockActivity> recentStockActivity,
    List<RecentPriceActivity> recentPriceActivity
) {
    public record RecentStockActivity(
        String productName,
        String storeName,
        String type,
        BigDecimal adjustmentQuantity,
        BigDecimal newQuantity,
        String note,
        OffsetDateTime changedAt
    ) {
    }

    public record RecentPriceActivity(
        String productName,
        String storeName,
        BigDecimal oldPrice,
        BigDecimal newPrice,
        String note,
        OffsetDateTime changedAt
    ) {
    }
}
