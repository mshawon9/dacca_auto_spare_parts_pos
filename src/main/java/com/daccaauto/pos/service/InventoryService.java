package com.daccaauto.pos.service;

import com.daccaauto.pos.dto.inventory.*;

import java.util.List;

public interface InventoryService {

    List<StoreResponse> getActiveStores();

    StoreResponse createStore(StoreCreateRequest request);

    InventoryPageResponse getInventory(Long storeId, Long categoryId, String keyword, int page);

    StockAdjustmentResponse adjustStock(StockAdjustmentRequest request);

    PriceUpdateResponse updatePrice(PriceUpdateRequest request);

    List<PriceHistoryResponse> getPriceHistory(Long storeId, Long productId);
}
