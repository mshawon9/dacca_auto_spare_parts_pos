package com.daccaauto.pos.controller;

import com.daccaauto.pos.dto.inventory.*;
import com.daccaauto.pos.service.InventoryService;
import com.daccaauto.pos.service.ProductCategoryService;
import com.daccaauto.pos.exception.DuplicateResourceException;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private static final String INVENTORY_STORE_SESSION_KEY = "inventoryStoreId";

    private final InventoryService inventoryService;
    private final ProductCategoryService productCategoryService;

    @GetMapping
    public String inventory(@RequestParam(required = false) Long storeId,
                            @RequestParam(required = false) Long categoryId,
                            @RequestParam(required = false) String keyword,
                            @RequestParam(defaultValue = "0") int page,
                            HttpSession session,
                            Model model) {
        Long selectedStoreId = (Long) session.getAttribute(INVENTORY_STORE_SESSION_KEY);
        if (selectedStoreId == null && storeId != null) {
            selectedStoreId = storeId;
            session.setAttribute(INVENTORY_STORE_SESSION_KEY, selectedStoreId);
        }

        java.util.List<StoreResponse> stores = inventoryService.getActiveStores();
        Long selectedStoreIdForLookup = selectedStoreId;
        StoreResponse selectedStore = stores.stream()
            .filter(store -> store.id().equals(selectedStoreIdForLookup))
            .findFirst()
            .orElse(null);

        if (selectedStoreId != null && selectedStore == null) {
            session.removeAttribute(INVENTORY_STORE_SESSION_KEY);
            selectedStoreId = null;
        }

        InventoryPageResponse inventoryPage = inventoryService.getInventory(selectedStoreId, categoryId, keyword, page);
        model.addAttribute("stores", stores);
        model.addAttribute("selectedStore", selectedStore);
        model.addAttribute("categories", productCategoryService.getAll());
        model.addAttribute("inventoryRows", inventoryPage.rows());
        model.addAttribute("inventoryPage", inventoryPage);
        model.addAttribute("selectedStoreId", selectedStoreId);
        model.addAttribute("selectedCategoryId", categoryId);
        model.addAttribute("keyword", keyword);
        return "inventory/list";
    }

    @PostMapping("/change-store")
    public String changeStore(HttpSession session) {
        session.removeAttribute(INVENTORY_STORE_SESSION_KEY);
        return "redirect:/inventory";
    }

    @PostMapping("/stores")
    @ResponseBody
    public StoreResponse createStore(@Valid @RequestBody StoreCreateRequest request) {
        return inventoryService.createStore(request);
    }

    @PostMapping("/adjust")
    @ResponseBody
    public StockAdjustmentResponse adjustStock(@Valid @RequestBody StockAdjustmentRequest request,
                                               HttpSession session) {
        validateLockedStore(request.storeId(), session);
        return inventoryService.adjustStock(request);
    }

    @PostMapping("/price")
    @ResponseBody
    public PriceUpdateResponse updatePrice(@Valid @RequestBody PriceUpdateRequest request,
                                           HttpSession session) {
        validateLockedStore(request.storeId(), session);
        return inventoryService.updatePrice(request);
    }

    @GetMapping("/price-history")
    @ResponseBody
    public java.util.List<PriceHistoryResponse> priceHistory(@RequestParam Long storeId,
                                                            @RequestParam Long productId,
                                                            HttpSession session) {
        validateLockedStore(storeId, session);
        return inventoryService.getPriceHistory(storeId, productId);
    }

    private void validateLockedStore(Long requestStoreId, HttpSession session) {
        Long lockedStoreId = (Long) session.getAttribute(INVENTORY_STORE_SESSION_KEY);
        if (lockedStoreId == null) {
            throw new DuplicateResourceException("Select and lock a warehouse before updating inventory");
        }
        if (!lockedStoreId.equals(requestStoreId)) {
            throw new DuplicateResourceException(
                "Warehouse mismatch detected. Reload the inventory screen before updating."
            );
        }
    }
}
