package com.daccaauto.pos.service.serviceImpl;

import com.daccaauto.pos.dto.inventory.*;
import com.daccaauto.pos.entity.*;
import com.daccaauto.pos.exception.DuplicateResourceException;
import com.daccaauto.pos.exception.ResourceNotFoundException;
import com.daccaauto.pos.repository.*;
import com.daccaauto.pos.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class InventoryServiceImpl implements InventoryService {

    private static final int QUANTITY_SCALE = 0;
    private static final int PRICE_SCALE = 2;
    private static final int INVENTORY_PAGE_SIZE = 15;

    private final StoreRepository storeRepository;
    private final ProductRepository productRepository;
    private final ProductStockRepository productStockRepository;
    private final StockAdjustmentRepository stockAdjustmentRepository;
    private final ProductPriceHistoryRepository productPriceHistoryRepository;

    @Override
    @Transactional(readOnly = true)
    public List<StoreResponse> getActiveStores() {
        return storeRepository.findAllByActiveTrueOrderByNameAsc()
            .stream()
            .map(this::mapStore)
            .toList();
    }

    @Override
    public StoreResponse createStore(StoreCreateRequest request) {
        String name = request.name().trim();
        if (storeRepository.existsByNameIgnoreCase(name)) {
            throw new DuplicateResourceException("A store with this name already exists");
        }

        StoreEntity store = new StoreEntity();
        store.setName(name);
        store.setCode(trimToNull(request.code()));
        store.setAddress(trimToNull(request.address()));
        store.setActive(true);
        return mapStore(storeRepository.save(store));
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryPageResponse getInventory(Long storeId, Long categoryId, String keyword, int page) {
        if (storeId == null) {
            return InventoryPageResponse.empty(INVENTORY_PAGE_SIZE);
        }
        requireActiveStore(storeId);

        String keywordPattern = keyword == null || keyword.isBlank()
            ? null
            : "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";

        int safePage = Math.max(page, 0);
        Page<ProductEntity> productPage = productRepository.searchInventoryPage(
            keywordPattern,
            categoryId,
            PageRequest.of(
                safePage,
                INVENTORY_PAGE_SIZE,
                Sort.by(Sort.Order.asc("name"), Sort.Order.asc("partNumber"))
            )
        );
        List<ProductEntity> products = productPage.getContent();

        if (products.isEmpty()) {
            return new InventoryPageResponse(
                List.of(),
                productPage.getNumber(),
                productPage.getSize(),
                productPage.getTotalPages(),
                productPage.getTotalElements(),
                productPage.isFirst(),
                productPage.isLast()
            );
        }

        List<Long> productIds = products.stream().map(ProductEntity::getId).toList();
        Map<Long, ProductStockEntity> stockByProductId = productStockRepository
            .findByStoreIdAndProductIdIn(storeId, productIds)
            .stream()
            .collect(Collectors.toMap(stock -> stock.getProduct().getId(), Function.identity()));

        List<InventoryRowResponse> rows = products.stream()
            .map(product -> new InventoryRowResponse(
                product.getId(),
                product.getName(),
                product.getPartNumber(),
                product.getSku(),
                product.getCategory().getName(),
                product.getBrand().getName(),
                product.getImageFileName() != null,
                stockByProductId.getOrDefault(product.getId(), emptyStock()).getQuantity(),
                stockByProductId.getOrDefault(product.getId(), emptyStock()).getSellingPrice(),
                previousPrice(storeId, product.getId()),
                stockByProductId.getOrDefault(product.getId(), emptyStock()).getCostPrice(),
                previousCostPrice(storeId, product.getId())
            ))
            .toList();

        return new InventoryPageResponse(
            rows,
            productPage.getNumber(),
            productPage.getSize(),
            productPage.getTotalPages(),
            productPage.getTotalElements(),
            productPage.isFirst(),
            productPage.isLast()
        );
    }

    @Override
    public StockAdjustmentResponse adjustStock(StockAdjustmentRequest request) {
        StoreEntity store = requireActiveStore(request.storeId());
        ProductEntity product = productRepository.findById(request.productId())
            .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + request.productId()));

        if (!product.isActive()) {
            throw new DuplicateResourceException("Stock cannot be changed for an inactive product");
        }

        BigDecimal requestedQuantity = normalizeQuantity(request.quantity());
        validateAdjustmentQuantity(request.adjustmentType(), requestedQuantity);

        ProductStockEntity stock = productStockRepository
            .findForUpdate(store.getId(), product.getId())
            .orElseGet(() -> newStock(store, product));

        BigDecimal previousQuantity = normalizeQuantity(stock.getQuantity());
        BigDecimal newQuantity = calculateNewQuantity(
            request.adjustmentType(),
            previousQuantity,
            requestedQuantity
        );

        if (newQuantity.compareTo(previousQuantity) == 0) {
            throw new DuplicateResourceException("Stock quantity is already " + formatQuantity(newQuantity));
        }

        stock.setQuantity(newQuantity);
        productStockRepository.save(stock);

        BigDecimal adjustmentQuantity = newQuantity.subtract(previousQuantity);
        StockAdjustmentEntity adjustment = new StockAdjustmentEntity();
        adjustment.setStore(store);
        adjustment.setProduct(product);
        adjustment.setAdjustmentType(request.adjustmentType());
        adjustment.setPreviousQuantity(previousQuantity);
        adjustment.setAdjustmentQuantity(adjustmentQuantity);
        adjustment.setNewQuantity(newQuantity);
        adjustment.setNote(trimToNull(request.note()));
        stockAdjustmentRepository.save(adjustment);

        return new StockAdjustmentResponse(
            product.getId(),
            store.getId(),
            request.adjustmentType(),
            previousQuantity,
            adjustmentQuantity,
            newQuantity
        );
    }

    @Override
    public PriceUpdateResponse updatePrice(PriceUpdateRequest request) {
        StoreEntity store = requireActiveStore(request.storeId());
        ProductEntity product = requireActiveProduct(request.productId());
        BigDecimal newPrice = normalizePrice(request.price());
        BigDecimal newCostPrice = request.costPrice() == null ? null : normalizePrice(request.costPrice());

        ProductStockEntity stock = productStockRepository
            .findForUpdate(store.getId(), product.getId())
            .orElseGet(() -> newStock(store, product));

        BigDecimal oldPrice = stock.getSellingPrice();
        BigDecimal oldCostPrice = stock.getCostPrice();
        boolean sellingPriceChanged = oldPrice == null || oldPrice.compareTo(newPrice) != 0;
        boolean costPriceChanged = newCostPrice != null
            && (oldCostPrice == null || oldCostPrice.compareTo(newCostPrice) != 0);

        if (!sellingPriceChanged && !costPriceChanged) {
            throw new DuplicateResourceException("Product price information is already up to date");
        }

        stock.setSellingPrice(newPrice);
        if (newCostPrice != null) {
            stock.setCostPrice(newCostPrice);
        }
        productStockRepository.save(stock);

        ProductPriceHistoryEntity history = new ProductPriceHistoryEntity();
        history.setStore(store);
        history.setProduct(product);
        history.setOldPrice(oldPrice);
        history.setNewPrice(newPrice);
        history.setOldCostPrice(oldCostPrice);
        history.setNewCostPrice(newCostPrice == null ? oldCostPrice : newCostPrice);
        history.setNote(trimToNull(request.note()));
        productPriceHistoryRepository.save(history);

        return new PriceUpdateResponse(
            store.getId(),
            product.getId(),
            oldPrice,
            newPrice,
            oldCostPrice,
            newCostPrice == null ? oldCostPrice : newCostPrice
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<PriceHistoryResponse> getPriceHistory(Long storeId, Long productId) {
        requireActiveStore(storeId);
        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("Product not found: " + productId);
        }

        return productPriceHistoryRepository
            .findTop10ByStoreIdAndProductIdOrderByCreatedAtDesc(storeId, productId)
            .stream()
            .map(history -> new PriceHistoryResponse(
                history.getOldPrice(),
                history.getNewPrice(),
                history.getOldCostPrice(),
                history.getNewCostPrice(),
                history.getNote(),
                history.getCreatedAt()
            ))
            .toList();
    }

    private StoreEntity requireActiveStore(Long storeId) {
        StoreEntity store = storeRepository.findById(storeId)
            .orElseThrow(() -> new ResourceNotFoundException("Store not found: " + storeId));
        if (!store.isActive()) {
            throw new DuplicateResourceException("Selected store is inactive");
        }
        return store;
    }

    private ProductEntity requireActiveProduct(Long productId) {
        ProductEntity product = productRepository.findById(productId)
            .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + productId));
        if (!product.isActive()) {
            throw new DuplicateResourceException("Price cannot be changed for an inactive product");
        }
        return product;
    }

    private BigDecimal calculateNewQuantity(StockAdjustmentType type,
                                            BigDecimal currentQuantity,
                                            BigDecimal requestedQuantity) {
        return switch (type) {
            case INCREASE -> currentQuantity.add(requestedQuantity);
            case DECREASE -> {
                if (requestedQuantity.compareTo(currentQuantity) > 0) {
                    throw new DuplicateResourceException(
                        "Cannot decrease by " + formatQuantity(requestedQuantity)
                            + ". Available stock is " + formatQuantity(currentQuantity)
                    );
                }
                yield currentQuantity.subtract(requestedQuantity);
            }
            case SET -> requestedQuantity;
        };
    }

    private void validateAdjustmentQuantity(StockAdjustmentType type, BigDecimal quantity) {
        if (type != StockAdjustmentType.SET && quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new DuplicateResourceException("Increase or decrease quantity must be greater than zero");
        }
    }

    private BigDecimal normalizeQuantity(BigDecimal quantity) {
        try {
            return quantity.setScale(QUANTITY_SCALE, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException ex) {
            throw new DuplicateResourceException("Quantity must be a whole number");
        }
    }

    private BigDecimal normalizePrice(BigDecimal price) {
        return price.setScale(PRICE_SCALE, RoundingMode.UNNECESSARY);
    }

    private String formatQuantity(BigDecimal quantity) {
        return quantity.stripTrailingZeros().toPlainString();
    }

    private String formatPrice(BigDecimal price) {
        return price.setScale(PRICE_SCALE).toPlainString();
    }

    private ProductStockEntity newStock(StoreEntity store, ProductEntity product) {
        ProductStockEntity stock = new ProductStockEntity();
        stock.setStore(store);
        stock.setProduct(product);
        stock.setQuantity(BigDecimal.ZERO.setScale(QUANTITY_SCALE));
        return stock;
    }

    private ProductStockEntity emptyStock() {
        ProductStockEntity stock = new ProductStockEntity();
        stock.setQuantity(BigDecimal.ZERO.setScale(QUANTITY_SCALE));
        return stock;
    }

    private BigDecimal previousPrice(Long storeId, Long productId) {
        ProductPriceHistoryEntity latest = productPriceHistoryRepository
            .findFirstByStoreIdAndProductIdOrderByCreatedAtDesc(storeId, productId);
        return latest == null ? null : latest.getOldPrice();
    }

    private BigDecimal previousCostPrice(Long storeId, Long productId) {
        ProductPriceHistoryEntity latest = productPriceHistoryRepository
            .findFirstByStoreIdAndProductIdOrderByCreatedAtDesc(storeId, productId);
        return latest == null ? null : latest.getOldCostPrice();
    }

    private StoreResponse mapStore(StoreEntity store) {
        return new StoreResponse(store.getId(), store.getName(), store.getCode(), store.getAddress());
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
