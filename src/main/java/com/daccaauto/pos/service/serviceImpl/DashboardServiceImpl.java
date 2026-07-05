package com.daccaauto.pos.service.serviceImpl;

import com.daccaauto.pos.dto.dashboard.DashboardSummaryResponse;
import com.daccaauto.pos.repository.*;
import com.daccaauto.pos.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardServiceImpl implements DashboardService {

    private final ProductRepository productRepository;
    private final ProductStockRepository productStockRepository;
    private final StoreRepository storeRepository;
    private final ProductCategoryRepository productCategoryRepository;
    private final BrandRepository brandRepository;
    private final VehicleApplicationRepository vehicleApplicationRepository;
    private final StockAdjustmentRepository stockAdjustmentRepository;
    private final ProductPriceHistoryRepository productPriceHistoryRepository;

    @Override
    public DashboardSummaryResponse getSummary() {
        var stockActivity = stockAdjustmentRepository.findTop8ByOrderByCreatedAtDesc()
            .stream()
            .map(item -> new DashboardSummaryResponse.RecentStockActivity(
                item.getProduct().getName(),
                item.getStore().getName(),
                item.getAdjustmentType().name(),
                item.getAdjustmentQuantity(),
                item.getNewQuantity(),
                item.getNote(),
                item.getCreatedAt()
            ))
            .toList();

        var priceActivity = productPriceHistoryRepository.findTop8ByOrderByCreatedAtDesc()
            .stream()
            .map(item -> new DashboardSummaryResponse.RecentPriceActivity(
                item.getProduct().getName(),
                item.getStore().getName(),
                item.getOldPrice(),
                item.getNewPrice(),
                item.getNote(),
                item.getCreatedAt()
            ))
            .toList();

        BigDecimal totalStock = productStockRepository.sumQuantity();
        var reorderAlerts = productRepository.findProductsAtOrBelowReorderLevel(PageRequest.of(0, 10))
            .stream()
            .map(item -> new DashboardSummaryResponse.ReorderLevelAlert(
                item.getProductId(),
                item.getProductName(),
                item.getPartNumber(),
                item.getTotalQuantity(),
                item.getReorderLevel()
            ))
            .toList();

        return new DashboardSummaryResponse(
            productRepository.count(),
            productRepository.countByActiveTrue(),
            storeRepository.countByActiveTrue(),
            productCategoryRepository.count(),
            brandRepository.count(),
            vehicleApplicationRepository.count(),
            totalStock == null ? BigDecimal.ZERO : totalStock,
            productStockRepository.countZeroQuantity(),
            productStockRepository.countBySellingPriceIsNull(),
            reorderAlerts,
            stockActivity,
            priceActivity
        );
    }
}
