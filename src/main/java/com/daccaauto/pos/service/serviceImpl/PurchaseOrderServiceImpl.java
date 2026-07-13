package com.daccaauto.pos.service.serviceImpl;

import com.daccaauto.pos.dto.purchase.*;
import com.daccaauto.pos.entity.*;
import com.daccaauto.pos.exception.DuplicateResourceException;
import com.daccaauto.pos.exception.ResourceNotFoundException;
import com.daccaauto.pos.repository.*;
import com.daccaauto.pos.service.OrderTodoService;
import com.daccaauto.pos.service.PurchaseOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class PurchaseOrderServiceImpl implements PurchaseOrderService {

    private static final int QUANTITY_SCALE = 0;
    private static final int PRICE_SCALE = 2;

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PurchaseOrderLineRepository purchaseOrderLineRepository;
    private final SupplierRepository supplierRepository;
    private final StoreRepository storeRepository;
    private final ProductRepository productRepository;
    private final ProductStockRepository productStockRepository;
    private final StockAdjustmentRepository stockAdjustmentRepository;
    private final ProductSupplierRepository productSupplierRepository;
    private final OrderTodoService orderTodoService;

    @Value("${app.purchase.default-tax-percent:5.00}")
    private BigDecimal defaultTaxPercent;

    @Override
    public PurchaseOrderResponse create(PurchaseOrderRequest request) {
        SupplierEntity supplier = validateSupplier(request.getSupplierId());
        StoreEntity store = validateStore(request.getStoreId());
        String invoiceId = validateInvoice(request.getInvoiceId());
        validatePurchaseDate(request.getPurchaseDate());
        validateInvoiceUnique(supplier.getId(), invoiceId, null);
        PurchaseOrderEntity order = new PurchaseOrderEntity();
        order.setSupplier(supplier);
        order.setStore(store);
        order.setPurchaseDate(request.getPurchaseDate());
        order.setInvoiceId(invoiceId);

        applyLines(order, supplier, store, validateLines(request.getLines()), invoiceId);

        PurchaseOrderEntity saved = purchaseOrderRepository.save(order);
        return new PurchaseOrderResponse(saved.getId(), saved.getInvoiceId(), saved.getTotal(), saved.getLines().size());
    }

    @Override
    public PurchaseOrderResponse update(Long id, PurchaseOrderRequest request) {
        PurchaseOrderEntity order = purchaseOrderRepository.findWithLinesById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Purchase order not found: " + id));
        if (hasReturns(order)) {
            throw new DuplicateResourceException("This PO has returned items. Edit is blocked to keep return history accurate.");
        }

        SupplierEntity supplier = validateSupplier(request.getSupplierId());
        StoreEntity store = validateStore(request.getStoreId());
        String invoiceId = validateInvoice(request.getInvoiceId());
        validatePurchaseDate(request.getPurchaseDate());
        validateInvoiceUnique(supplier.getId(), invoiceId, id);
        List<ValidatedLine> newLines = validateLines(request.getLines());

        List<PurchaseOrderLineEntity> oldLines = new ArrayList<>(order.getLines());
        for (PurchaseOrderLineEntity oldLine : oldLines) {
            decreaseStock(order.getStore(), oldLine.getProduct(), oldLine.getQuantity(),
                "Purchase invoice " + order.getInvoiceId() + " edit reversal");
        }

        order.getLines().clear();
        order.setSupplier(supplier);
        order.setStore(store);
        order.setPurchaseDate(request.getPurchaseDate());
        order.setInvoiceId(invoiceId);
        order.setTotal(BigDecimal.ZERO.setScale(PRICE_SCALE));

        applyLines(order, supplier, store, newLines, invoiceId);
        PurchaseOrderEntity saved = purchaseOrderRepository.save(order);
        return new PurchaseOrderResponse(saved.getId(), saved.getInvoiceId(), saved.getTotal(), saved.getLines().size());
    }

    @Override
    @Transactional(readOnly = true)
    public PurchaseOrderDetail getDetail(Long id) {
        return mapDetail(purchaseOrderRepository.findWithLinesById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Purchase order not found: " + id)));
    }

    @Override
    @Transactional(readOnly = true)
    public PurchaseOrderRequest getForm(Long id) {
        PurchaseOrderEntity order = purchaseOrderRepository.findWithLinesById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Purchase order not found: " + id));
        if (hasReturns(order)) {
            throw new DuplicateResourceException("This PO has returned items. Edit is blocked to keep return history accurate.");
        }
        PurchaseOrderRequest form = new PurchaseOrderRequest();
        form.setId(order.getId());
        form.setSupplierId(order.getSupplier().getId());
        form.setStoreId(order.getStore().getId());
        form.setPurchaseDate(order.getPurchaseDate());
        form.setInvoiceId(order.getInvoiceId());
        List<PurchaseOrderRequest.Line> lines = new ArrayList<>();
        for (PurchaseOrderLineEntity line : order.getLines()) {
            PurchaseOrderRequest.Line requestLine = new PurchaseOrderRequest.Line();
            requestLine.setId(line.getId());
            requestLine.setProductId(line.getProduct().getId());
            requestLine.setProductText(buildProductDisplayName(line.getProduct()));
            requestLine.setSupplierProductCode(line.getSupplierProductCode());
            requestLine.setQuantity(line.getQuantity());
            requestLine.setUnitPrice(line.getUnitPrice());
            requestLine.setTaxPercent(line.getTaxPercent());
            lines.add(requestLine);
        }
        form.setLines(lines);
        return form;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PurchaseOrderListItem> search(String keyword, Pageable pageable) {
        String pattern = trimToNull(keyword);
        String keywordPattern = pattern == null ? null : "%" + pattern.toLowerCase() + "%";
        return purchaseOrderRepository.search(keywordPattern, pageable).map(this::mapListItem);
    }

    @Override
    public void returnItem(Long orderId, PurchaseReturnRequest request) {
        PurchaseOrderLineEntity line = purchaseOrderLineRepository.findWithPurchaseOrderAndProductById(request.getLineId())
            .orElseThrow(() -> new ResourceNotFoundException("Purchase order line not found: " + request.getLineId()));
        if (!line.getPurchaseOrder().getId().equals(orderId)) {
            throw new DuplicateResourceException("Selected return item does not belong to this purchase order");
        }
        BigDecimal quantity = normalizeQuantity(request.getQuantity());
        BigDecimal returned = line.getReturnedQuantity() == null ? BigDecimal.ZERO.setScale(QUANTITY_SCALE) : line.getReturnedQuantity();
        BigDecimal returnable = line.getQuantity().subtract(returned);
        if (quantity.compareTo(returnable) > 0) {
            throw new DuplicateResourceException("Return quantity cannot be more than available return quantity");
        }

        decreaseStock(line.getPurchaseOrder().getStore(), line.getProduct(), quantity,
            "Purchase invoice " + line.getPurchaseOrder().getInvoiceId() + " return");
        line.setReturnedQuantity(returned.add(quantity));
        purchaseOrderLineRepository.save(line);
        orderTodoService.cleanupForProduct(line.getProduct().getId());
    }

    private void applyLines(PurchaseOrderEntity order,
                            SupplierEntity supplier,
                            StoreEntity store,
                            List<ValidatedLine> lines,
                            String invoiceId) {
        if (lines.isEmpty()) {
            throw new DuplicateResourceException("Add at least one purchase item");
        }

        BigDecimal total = BigDecimal.ZERO.setScale(PRICE_SCALE);
        for (ValidatedLine line : lines) {
            ProductEntity product = productRepository.findById(line.productId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + line.productId()));
            if (!product.isActive()) {
                throw new DuplicateResourceException("Product is inactive: " + product.getName());
            }

            BigDecimal subTotal = line.quantity().multiply(line.unitPrice()).setScale(PRICE_SCALE, RoundingMode.HALF_UP);
            BigDecimal taxAmount = subTotal.multiply(line.taxPercent())
                .divide(BigDecimal.valueOf(100), PRICE_SCALE, RoundingMode.HALF_UP);
            BigDecimal lineTotal = subTotal.add(taxAmount).setScale(PRICE_SCALE, RoundingMode.HALF_UP);
            total = total.add(lineTotal);

            PurchaseOrderLineEntity orderLine = new PurchaseOrderLineEntity();
            orderLine.setPurchaseOrder(order);
            orderLine.setProduct(product);
            orderLine.setSupplierProductCode(line.supplierProductCode());
            orderLine.setQuantity(line.quantity());
            orderLine.setReturnedQuantity(BigDecimal.ZERO.setScale(QUANTITY_SCALE));
            orderLine.setUnitPrice(line.unitPrice());
            orderLine.setTaxPercent(line.taxPercent());
            orderLine.setTaxAmount(taxAmount);
            orderLine.setLineTotal(lineTotal);
            order.getLines().add(orderLine);

            upsertSupplierProduct(supplier, product, line.supplierProductCode(), line.unitPrice());
            increaseStock(store, product, line.quantity(), line.unitPrice(), "Purchase invoice " + invoiceId);
        }
        order.setTotal(total.setScale(PRICE_SCALE, RoundingMode.HALF_UP));
    }

    private List<ValidatedLine> validateLines(List<PurchaseOrderRequest.Line> requestLines) {
        List<ValidatedLine> lines = new ArrayList<>();
        if (requestLines == null) {
            return lines;
        }

        for (PurchaseOrderRequest.Line line : requestLines) {
            if (line.getProductId() == null && line.getQuantity() == null && line.getUnitPrice() == null) {
                continue;
            }
            if (line.getProductId() == null) {
                throw new DuplicateResourceException("Product is required for every purchase row");
            }
            BigDecimal quantity = normalizeQuantity(line.getQuantity());
            BigDecimal unitPrice = normalizePrice(line.getUnitPrice());
            BigDecimal taxPercent = normalizeTaxPercent(line.getTaxPercent());
            if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
                throw new DuplicateResourceException("Quantity must be greater than zero");
            }
            if (unitPrice.compareTo(BigDecimal.ZERO) <= 0) {
                throw new DuplicateResourceException("Unit price must be greater than zero");
            }
            if (taxPercent.compareTo(BigDecimal.ZERO) < 0) {
                throw new DuplicateResourceException("Tax cannot be negative");
            }
            lines.add(new ValidatedLine(
                line.getProductId(),
                trimToNull(line.getSupplierProductCode()),
                quantity,
                unitPrice,
                taxPercent
            ));
        }
        return lines;
    }

    private void upsertSupplierProduct(SupplierEntity supplier,
                                       ProductEntity product,
                                       String supplierProductCode,
                                       BigDecimal unitPrice) {
        productSupplierRepository.findByProductIdAndSupplierId(product.getId(), supplier.getId())
            .ifPresentOrElse(existing -> {
                existing.setSupplierProductCode(supplierProductCode);
                existing.setPriceInput(unitPrice.toPlainString());
                existing.setPriceValue(unitPrice);
                existing.setCurrency("AED");
                existing.setActive(true);
                productSupplierRepository.save(existing);
            }, () -> {
                ProductSupplierEntity mapping = new ProductSupplierEntity();
                mapping.setSupplier(supplier);
                mapping.setProduct(product);
                mapping.setSupplierProductCode(supplierProductCode);
                mapping.setPriceInput(unitPrice.toPlainString());
                mapping.setPriceValue(unitPrice);
                mapping.setCurrency("AED");
                mapping.setActive(true);
                productSupplierRepository.save(mapping);
            });
    }

    private void increaseStock(StoreEntity store,
                               ProductEntity product,
                               BigDecimal quantity,
                               BigDecimal unitPrice,
                               String note) {
        ProductStockEntity stock = productStockRepository
            .findForUpdate(store.getId(), product.getId())
            .orElseGet(() -> newStock(store, product));
        BigDecimal previousQuantity = stock.getQuantity() == null
            ? BigDecimal.ZERO.setScale(QUANTITY_SCALE)
            : stock.getQuantity().setScale(QUANTITY_SCALE);
        BigDecimal newQuantity = previousQuantity.add(quantity);

        stock.setQuantity(newQuantity);
        stock.setCostPrice(unitPrice);
        productStockRepository.save(stock);

        StockAdjustmentEntity adjustment = new StockAdjustmentEntity();
        adjustment.setStore(store);
        adjustment.setProduct(product);
        adjustment.setAdjustmentType(StockAdjustmentType.INCREASE);
        adjustment.setPreviousQuantity(previousQuantity);
        adjustment.setAdjustmentQuantity(quantity);
        adjustment.setNewQuantity(newQuantity);
        adjustment.setNote(note);
        stockAdjustmentRepository.save(adjustment);
        orderTodoService.cleanupForProduct(product.getId());
    }

    private void decreaseStock(StoreEntity store,
                               ProductEntity product,
                               BigDecimal quantity,
                               String note) {
        ProductStockEntity stock = productStockRepository
            .findForUpdate(store.getId(), product.getId())
            .orElseThrow(() -> new DuplicateResourceException("No stock found for " + product.getName() + " in selected warehouse"));
        BigDecimal previousQuantity = stock.getQuantity() == null
            ? BigDecimal.ZERO.setScale(QUANTITY_SCALE)
            : stock.getQuantity().setScale(QUANTITY_SCALE);
        BigDecimal newQuantity = previousQuantity.subtract(quantity);
        if (newQuantity.compareTo(BigDecimal.ZERO) < 0) {
            throw new DuplicateResourceException("Not enough stock to return or edit this item: " + product.getName());
        }

        stock.setQuantity(newQuantity);
        productStockRepository.save(stock);

        StockAdjustmentEntity adjustment = new StockAdjustmentEntity();
        adjustment.setStore(store);
        adjustment.setProduct(product);
        adjustment.setAdjustmentType(StockAdjustmentType.DECREASE);
        adjustment.setPreviousQuantity(previousQuantity);
        adjustment.setAdjustmentQuantity(quantity);
        adjustment.setNewQuantity(newQuantity);
        adjustment.setNote(note);
        stockAdjustmentRepository.save(adjustment);
    }

    private ProductStockEntity newStock(StoreEntity store, ProductEntity product) {
        ProductStockEntity stock = new ProductStockEntity();
        stock.setStore(store);
        stock.setProduct(product);
        stock.setQuantity(BigDecimal.ZERO.setScale(QUANTITY_SCALE));
        return stock;
    }

    private BigDecimal normalizeQuantity(BigDecimal quantity) {
        if (quantity == null) {
            throw new DuplicateResourceException("Quantity is required");
        }
        try {
            return quantity.setScale(QUANTITY_SCALE, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException ex) {
            throw new DuplicateResourceException("Quantity must be a whole number");
        }
    }

    private BigDecimal normalizePrice(BigDecimal price) {
        if (price == null) {
            throw new DuplicateResourceException("Unit price is required");
        }
        try {
            return price.setScale(PRICE_SCALE, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException ex) {
            throw new DuplicateResourceException("Unit price can have maximum 2 decimals");
        }
    }

    private BigDecimal normalizeTaxPercent(BigDecimal taxPercent) {
        if (taxPercent == null) {
            return defaultTaxPercent.setScale(PRICE_SCALE, RoundingMode.UNNECESSARY);
        }
        try {
            return taxPercent.setScale(PRICE_SCALE, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException ex) {
            throw new DuplicateResourceException("Tax can have maximum 2 decimals");
        }
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private SupplierEntity validateSupplier(Long supplierId) {
        SupplierEntity supplier = supplierRepository.findById(supplierId)
            .orElseThrow(() -> new ResourceNotFoundException("Supplier not found: " + supplierId));
        if (!supplier.isActive()) {
            throw new DuplicateResourceException("Selected supplier is inactive");
        }
        return supplier;
    }

    private StoreEntity validateStore(Long storeId) {
        StoreEntity store = storeRepository.findById(storeId)
            .orElseThrow(() -> new ResourceNotFoundException("Store not found: " + storeId));
        if (!store.isActive()) {
            throw new DuplicateResourceException("Selected warehouse is inactive");
        }
        return store;
    }

    private String validateInvoice(String invoiceIdValue) {
        String invoiceId = trimToNull(invoiceIdValue);
        if (invoiceId == null) {
            throw new DuplicateResourceException("Invoice ID is required");
        }
        return invoiceId;
    }

    private void validatePurchaseDate(LocalDate purchaseDate) {
        if (purchaseDate == null) {
            throw new DuplicateResourceException("Purchase date is required");
        }
    }

    private void validateInvoiceUnique(Long supplierId, String invoiceId, Long excludeId) {
        boolean exists = excludeId == null
            ? purchaseOrderRepository.existsBySupplierIdAndInvoiceIdIgnoreCase(supplierId, invoiceId)
            : purchaseOrderRepository.existsBySupplierIdAndInvoiceIdIgnoreCaseAndIdNot(supplierId, invoiceId, excludeId);
        if (exists) {
            throw new DuplicateResourceException("This supplier invoice already exists: " + invoiceId);
        }
    }

    private PurchaseOrderListItem mapListItem(PurchaseOrderEntity order) {
        BigDecimal returnedQuantity = order.getLines().stream()
            .map(line -> line.getReturnedQuantity() == null ? BigDecimal.ZERO : line.getReturnedQuantity())
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new PurchaseOrderListItem(
            order.getId(),
            order.getInvoiceId(),
            order.getPurchaseDate(),
            order.getSupplier().getName(),
            order.getStore().getName(),
            order.getTotal(),
            order.getLines().size(),
            returnedQuantity,
            returnedQuantity.compareTo(BigDecimal.ZERO) > 0
        );
    }

    private PurchaseOrderDetail mapDetail(PurchaseOrderEntity order) {
        List<PurchaseOrderDetail.Line> lines = order.getLines().stream()
            .map(line -> {
                BigDecimal returned = line.getReturnedQuantity() == null ? BigDecimal.ZERO.setScale(QUANTITY_SCALE) : line.getReturnedQuantity();
                return new PurchaseOrderDetail.Line(
                    line.getId(),
                    line.getProduct().getId(),
                    line.getProduct().getName(),
                    line.getProduct().getPartNumber(),
                    line.getProduct().getBrand().getName(),
                    line.getSupplierProductCode(),
                    line.getQuantity(),
                    returned,
                    line.getQuantity().subtract(returned),
                    line.getUnitPrice(),
                    line.getTaxPercent(),
                    line.getTaxAmount(),
                    line.getLineTotal()
                );
            })
            .toList();
        return new PurchaseOrderDetail(
            order.getId(),
            order.getInvoiceId(),
            order.getPurchaseDate(),
            order.getSupplier().getId(),
            order.getSupplier().getName(),
            order.getStore().getId(),
            order.getStore().getName(),
            order.getTotal(),
            hasReturns(order),
            lines
        );
    }

    private boolean hasReturns(PurchaseOrderEntity order) {
        return order.getLines().stream()
            .anyMatch(line -> line.getReturnedQuantity() != null && line.getReturnedQuantity().compareTo(BigDecimal.ZERO) > 0);
    }

    private String buildProductDisplayName(ProductEntity product) {
        return java.util.stream.Stream.of(
                product.getCategory() == null ? null : product.getCategory().getName(),
                product.getName(),
                product.getBrand() == null ? null : "Brand: " + product.getBrand().getName(),
                "Part: " + product.getPartNumber()
            )
            .filter(value -> value != null && !value.isBlank())
            .collect(java.util.stream.Collectors.joining(" | "));
    }

    private record ValidatedLine(
        Long productId,
        String supplierProductCode,
        BigDecimal quantity,
        BigDecimal unitPrice,
        BigDecimal taxPercent
    ) {
    }
}
