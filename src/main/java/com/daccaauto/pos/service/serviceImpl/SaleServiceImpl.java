package com.daccaauto.pos.service.serviceImpl;

import com.daccaauto.pos.dto.sale.*;
import com.daccaauto.pos.entity.*;
import com.daccaauto.pos.exception.DuplicateResourceException;
import com.daccaauto.pos.exception.ResourceNotFoundException;
import com.daccaauto.pos.repository.*;
import com.daccaauto.pos.service.SaleService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Transactional
public class SaleServiceImpl implements SaleService {

    private static final int QUANTITY_SCALE = 0;
    private static final int PRICE_SCALE = 2;
    private static final Pattern YEAR_PATTERN = Pattern.compile("\\b(19\\d{2}|20\\d{2}|2100)\\b");

    private final SaleDraftRepository saleDraftRepository;
    private final SaleDraftLineRepository saleDraftLineRepository;
    private final SaleRepository saleRepository;
    private final SaleDraftActionRepository saleDraftActionRepository;
    private final CustomerRepository customerRepository;
    private final StoreRepository storeRepository;
    private final ProductRepository productRepository;
    private final ProductStockRepository productStockRepository;
    private final StockAdjustmentRepository stockAdjustmentRepository;
    private final CustomerProductPriceRepository customerProductPriceRepository;

    @Value("${app.sale.default-vat-percent:5.00}")
    private BigDecimal defaultVatPercent;

    @Override
    @Transactional(readOnly = true)
    public List<SaleDraftSummary> openDrafts() {
        return saleDraftRepository.findTop10ByOrderByUpdatedAtDesc()
            .stream()
            .map(this::mapSummary)
            .filter(summary -> summary.lineCount() > 0)
            .toList();
    }

    @Override
    public SaleDraftResponse createDraft() {
        StoreEntity store = storeRepository.findAllByActiveTrueOrderByNameAsc()
            .stream()
            .findFirst()
            .orElseThrow(() -> new DuplicateResourceException("Create an active warehouse before making sales"));
        SaleDraftEntity draft = new SaleDraftEntity();
        draft.setStore(store);
        draft.setSaleDate(LocalDate.now());
        draft.setSaleType(SaleType.REGULAR);
        draft.setVatMode(VatMode.EXCLUSIVE);
        draft.setVatPercent(defaultVatPercent.setScale(PRICE_SCALE, RoundingMode.HALF_UP));
        draft.setPaymentMethod(PaymentMethod.CASH);
        draft.setPaidAmount(BigDecimal.ZERO.setScale(PRICE_SCALE));
        SaleDraftEntity saved = saleDraftRepository.save(draft);
        recordAction(saved, "CREATE_DRAFT", "New sale draft created");
        return mapDraft(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public SaleDraftResponse getDraft(Long id) {
        return mapDraft(requireDraft(id));
    }

    @Override
    public SaleDraftResponse updateHeader(Long id, SaleDraftHeaderRequest request) {
        SaleDraftEntity draft = requireDraft(id);
        if (request.getCustomerId() != null) {
            CustomerEntity customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + request.getCustomerId()));
            if (!customer.isActive()) {
                throw new DuplicateResourceException("Selected customer is inactive");
            }
            draft.setCustomer(customer);
            if (customer.isAlwaysCredit()) {
                draft.setSaleType(SaleType.CREDIT_INSTALLMENT);
                draft.setPaymentMethod(PaymentMethod.CREDIT);
                draft.setPaidAmount(BigDecimal.ZERO.setScale(PRICE_SCALE));
            }
        } else {
            draft.setCustomer(null);
        }
        if (request.getStoreId() != null) {
            if (!draft.getLines().isEmpty() && !draft.getStore().getId().equals(request.getStoreId())) {
                throw new DuplicateResourceException("Warehouse is fixed after products are added. Remove products first to change warehouse.");
            }
            draft.setStore(requireActiveStore(request.getStoreId()));
        }
        if (request.getSaleDate() != null) {
            draft.setSaleDate(request.getSaleDate());
        }
        if (request.getSaleType() != null && (draft.getCustomer() == null || !draft.getCustomer().isAlwaysCredit())) {
            draft.setSaleType(request.getSaleType());
        }
        if (request.getVatMode() != null) {
            draft.setVatMode(request.getVatMode());
        }
        draft.setVatPercent(request.getVatPercent() == null
            ? defaultVatPercent.setScale(PRICE_SCALE, RoundingMode.HALF_UP)
            : normalizePrice(request.getVatPercent()));
        PaymentMethod paymentMethod = request.getPaymentMethod() == null ? PaymentMethod.CASH : request.getPaymentMethod();
        if (draft.getSaleType() != SaleType.REGULAR) {
            paymentMethod = PaymentMethod.CREDIT;
        }
        draft.setPaymentMethod(paymentMethod);
        draft.setPaidAmount(request.getPaidAmount() == null
            ? BigDecimal.ZERO.setScale(PRICE_SCALE)
            : normalizePrice(request.getPaidAmount()));
        draft.setNote(trimToNull(request.getNote()));
        SaleDraftEntity saved = saleDraftRepository.save(draft);
        recordAction(saved, "UPDATE_HEADER", "Header/customer/VAT/sale type updated");
        return mapDraft(saved);
    }

    @Override
    public SaleDraftResponse addLine(Long id, SaleDraftLineRequest request) {
        SaleDraftEntity draft = requireDraft(id);
        ProductEntity product = requireActiveProduct(request.getProductId());
        BigDecimal quantity = normalizeQuantity(request.getQuantity());
        BigDecimal unitPrice = normalizePrice(request.getUnitPrice());
        ProductStockEntity stock = productStockRepository.findForUpdate(draft.getStore().getId(), product.getId())
            .orElse(null);

        SaleDraftLineEntity line = draft.getLines().stream()
            .filter(existing -> existing.getProduct().getId().equals(product.getId())
                && existing.getUnitPrice().compareTo(unitPrice) == 0)
            .findFirst()
            .orElse(null);
        if (line == null) {
            line = new SaleDraftLineEntity();
            line.setDraft(draft);
            line.setProduct(product);
            line.setQuantity(quantity);
            line.setUnitPrice(unitPrice);
            line.setCostPrice(stock == null ? null : stock.getCostPrice());
            line.setActionNote("Added from sale screen");
            draft.getLines().add(line);
        } else {
            line.setQuantity(line.getQuantity().add(quantity));
            line.setCostPrice(stock == null ? line.getCostPrice() : stock.getCostPrice());
            line.setActionNote("Quantity updated from sale screen");
        }
        SaleDraftEntity saved = saleDraftRepository.save(draft);
        recordAction(saved, "ADD_LINE", "Product " + product.getId() + ", qty " + quantity + ", price " + unitPrice);
        return mapDraft(saved);
    }

    @Override
    public SaleDraftResponse removeLine(Long id, Long lineId) {
        SaleDraftEntity draft = requireDraft(id);
        boolean removed = draft.getLines().removeIf(line -> line.getId().equals(lineId));
        if (!removed) {
            throw new ResourceNotFoundException("Sale draft line not found: " + lineId);
        }
        recordAction(draft, "REMOVE_LINE", "Line " + lineId + " removed");
        saleDraftLineRepository.deleteById(lineId);
        return mapDraft(saleDraftRepository.save(draft));
    }

    @Override
    public SaleResponse complete(Long id) {
        SaleDraftEntity draft = requireDraft(id);
        if (draft.getLines().isEmpty()) {
            throw new DuplicateResourceException("Add at least one product before completing sale");
        }
        if (draft.getCustomer() == null && draft.getSaleType() != SaleType.REGULAR) {
            throw new DuplicateResourceException("Customer is required for credit sale");
        }

        SaleDraftResponse totals = mapDraft(draft);
        SaleEntity sale = new SaleEntity();
        sale.setInvoiceNo("SALE-" + System.currentTimeMillis());
        sale.setCustomer(draft.getCustomer());
        sale.setStore(draft.getStore());
        sale.setSaleDate(draft.getSaleDate());
        sale.setSaleType(draft.getSaleType());
        sale.setVatMode(draft.getVatMode());
        sale.setVatPercent(draft.getVatPercent());
        sale.setSubTotal(totals.subTotal());
        sale.setVatAmount(totals.vatAmount());
        sale.setTotal(totals.total());
        if (draft.getSaleType() == SaleType.REGULAR) {
            BigDecimal paidAmount = draft.getPaidAmount() == null || draft.getPaidAmount().compareTo(BigDecimal.ZERO) == 0
                ? totals.total()
                : draft.getPaidAmount().min(totals.total());
            sale.setPaymentMethod(draft.getPaymentMethod() == PaymentMethod.CREDIT ? PaymentMethod.CASH : draft.getPaymentMethod());
            sale.setPaidAmount(paidAmount);
            sale.setBalanceDue(totals.total().subtract(paidAmount).setScale(PRICE_SCALE, RoundingMode.HALF_UP));
        } else {
            sale.setPaymentMethod(PaymentMethod.CREDIT);
            sale.setPaidAmount(BigDecimal.ZERO.setScale(PRICE_SCALE));
            sale.setBalanceDue(totals.total());
            if (draft.getSaleType() == SaleType.MONTHLY_STATEMENT) {
                sale.setDueDate(draft.getSaleDate().plusMonths(1));
            }
        }
        sale.setNote(draft.getNote());

        for (SaleDraftLineEntity draftLine : draft.getLines()) {
            ProductStockEntity stock = productStockRepository
                .findForUpdate(draft.getStore().getId(), draftLine.getProduct().getId())
                .orElseGet(() -> newStock(draft.getStore(), draftLine.getProduct()));
            BigDecimal previousQuantity = stock.getQuantity();
            BigDecimal newQuantity = previousQuantity.subtract(draftLine.getQuantity()).max(BigDecimal.ZERO.setScale(QUANTITY_SCALE));
            stock.setQuantity(newQuantity);
            productStockRepository.save(stock);

            StockAdjustmentEntity adjustment = new StockAdjustmentEntity();
            adjustment.setStore(draft.getStore());
            adjustment.setProduct(draftLine.getProduct());
            adjustment.setAdjustmentType(StockAdjustmentType.DECREASE);
            adjustment.setPreviousQuantity(previousQuantity);
            adjustment.setAdjustmentQuantity(previousQuantity.subtract(newQuantity));
            adjustment.setNewQuantity(newQuantity);
            adjustment.setNote(draftLine.getQuantity().compareTo(previousQuantity) > 0
                ? "Sale invoice " + sale.getInvoiceNo() + " (sold " + draftLine.getQuantity().stripTrailingZeros().toPlainString() + ", stock floored at 0)"
                : "Sale invoice " + sale.getInvoiceNo());
            stockAdjustmentRepository.save(adjustment);

            SaleLineEntity line = new SaleLineEntity();
            line.setSale(sale);
            line.setProduct(draftLine.getProduct());
            line.setQuantity(draftLine.getQuantity());
            line.setUnitPrice(draftLine.getUnitPrice());
            line.setCostPrice(draftLine.getCostPrice());
            line.setLineTotal(calculateLineTotal(draftLine.getQuantity(), draftLine.getUnitPrice(), draft.getVatMode(), draft.getVatPercent()).total());
            sale.getLines().add(line);

            if (draft.getCustomer() != null) {
                upsertCustomerPrice(draft.getCustomer(), draftLine.getProduct(), draftLine.getUnitPrice());
            }
        }

        recordAction(draft, "COMPLETE_SALE", "Completing sale " + sale.getInvoiceNo());
        SaleEntity saved = saleRepository.save(sale);
        saleDraftActionRepository.deleteByDraftId(draft.getId());
        saleDraftRepository.delete(draft);
        return new SaleResponse(saved.getId(), saved.getInvoiceNo(), saved.getTotal());
    }

    @Override
    @Transactional(readOnly = true)
    public List<SelectOption> searchCustomers(String keyword) {
        String term = trimToNull(keyword);
        if (term == null || term.length() < 2) {
            return List.of();
        }
        String pattern = term.toLowerCase(Locale.ROOT);
        return customerRepository.findAll().stream()
            .filter(CustomerEntity::isActive)
            .filter(customer -> contains(customer.getName(), pattern)
                || contains(customer.getPhone(), pattern)
                || contains(customer.getTrnNumber(), pattern))
            .sorted(Comparator.comparing(CustomerEntity::getName))
            .limit(20)
            .map(customer -> new SelectOption(customer.getId(), customer.getName()))
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SaleProductOption> searchProducts(String keyword, Long storeId, Long customerId) {
        String term = trimToNull(keyword);
        if (term == null || term.length() < 2 || storeId == null) {
            return List.of();
        }
        String keywordPattern = "%" + term.toLowerCase(Locale.ROOT) + "%";
        ApplicationKeyword applicationKeyword = parseApplicationKeyword(term);
        return productRepository.search(
                keywordPattern,
                applicationKeyword.pattern(),
                applicationKeyword.year(),
                null,
                null,
                null,
                true
            ).stream()
            .limit(20)
            .map(product -> mapProductOption(product, storeId, customerId))
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public SaleProductOption productInfo(Long productId, Long storeId, Long customerId) {
        ProductEntity product = productRepository.findById(productId)
            .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + productId));
        return mapProductOption(product, storeId, customerId);
    }

    private SaleDraftEntity requireDraft(Long id) {
        return saleDraftRepository.findWithLinesById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Sale draft not found: " + id));
    }

    private ProductStockEntity newStock(StoreEntity store, ProductEntity product) {
        ProductStockEntity stock = new ProductStockEntity();
        stock.setStore(store);
        stock.setProduct(product);
        stock.setQuantity(BigDecimal.ZERO.setScale(QUANTITY_SCALE));
        return stock;
    }

    private StoreEntity requireActiveStore(Long storeId) {
        StoreEntity store = storeRepository.findById(storeId)
            .orElseThrow(() -> new ResourceNotFoundException("Store not found: " + storeId));
        if (!store.isActive()) {
            throw new DuplicateResourceException("Selected warehouse is inactive");
        }
        return store;
    }

    private ProductEntity requireActiveProduct(Long productId) {
        ProductEntity product = productRepository.findById(productId)
            .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + productId));
        if (!product.isActive()) {
            throw new DuplicateResourceException("Selected product is inactive");
        }
        return product;
    }

    private void upsertCustomerPrice(CustomerEntity customer, ProductEntity product, BigDecimal unitPrice) {
        customerProductPriceRepository.findByCustomerIdAndProductId(customer.getId(), product.getId())
            .ifPresentOrElse(existing -> {
                existing.setUnitPrice(unitPrice);
                customerProductPriceRepository.save(existing);
            }, () -> {
                CustomerProductPriceEntity price = new CustomerProductPriceEntity();
                price.setCustomer(customer);
                price.setProduct(product);
                price.setUnitPrice(unitPrice);
                customerProductPriceRepository.save(price);
            });
    }

    private SaleProductOption mapProductOption(ProductEntity product, Long storeId, Long customerId) {
        ProductStockEntity stock = storeId == null ? null : productStockRepository
            .findByStoreIdAndProductId(storeId, product.getId())
            .orElse(null);
        BigDecimal customerPrice = customerId == null ? null : customerProductPriceRepository
            .findByCustomerIdAndProductId(customerId, product.getId())
            .map(CustomerProductPriceEntity::getUnitPrice)
            .orElse(null);
        return new SaleProductOption(
            product.getId(),
            buildProductDisplayName(product),
            stock == null ? BigDecimal.ZERO.setScale(QUANTITY_SCALE) : stock.getQuantity(),
            stock == null ? null : stock.getCostPrice(),
            stock == null ? null : stock.getSellingPrice(),
            customerPrice
        );
    }

    private SaleDraftResponse mapDraft(SaleDraftEntity draft) {
        BigDecimal subTotal = BigDecimal.ZERO.setScale(PRICE_SCALE);
        BigDecimal vatAmount = BigDecimal.ZERO.setScale(PRICE_SCALE);
        BigDecimal total = BigDecimal.ZERO.setScale(PRICE_SCALE);
        List<SaleDraftResponse.Line> lines = draft.getLines().stream()
            .map(line -> {
                LineAmounts amounts = calculateLineTotal(line.getQuantity(), line.getUnitPrice(), draft.getVatMode(), draft.getVatPercent());
                return new SaleDraftResponse.Line(
                    line.getId(),
                    line.getProduct().getId(),
                    buildProductDisplayName(line.getProduct()),
                    line.getQuantity(),
                    line.getUnitPrice(),
                    line.getCostPrice(),
                    amounts.vatAmount(),
                    amounts.total()
                );
            })
            .toList();
        for (SaleDraftResponse.Line line : lines) {
            LineAmounts amounts = calculateLineTotal(line.quantity(), line.unitPrice(), draft.getVatMode(), draft.getVatPercent());
            subTotal = subTotal.add(amounts.subTotal());
            vatAmount = vatAmount.add(amounts.vatAmount());
            total = total.add(amounts.total());
        }
        BigDecimal normalizedTotal = total.setScale(PRICE_SCALE, RoundingMode.HALF_UP);
        BigDecimal paidAmount = draft.getPaidAmount() == null ? BigDecimal.ZERO.setScale(PRICE_SCALE) : draft.getPaidAmount();
        if (draft.getSaleType() != SaleType.REGULAR) {
            paidAmount = BigDecimal.ZERO.setScale(PRICE_SCALE);
        } else if (paidAmount.compareTo(BigDecimal.ZERO) == 0 && !lines.isEmpty()) {
            paidAmount = normalizedTotal;
        } else if (paidAmount.compareTo(normalizedTotal) > 0) {
            paidAmount = normalizedTotal;
        }
        return new SaleDraftResponse(
            draft.getId(),
            draft.getCustomer() == null ? null : draft.getCustomer().getId(),
            draft.getCustomer() == null ? null : draft.getCustomer().getName(),
            draft.getStore().getId(),
            draft.getStore().getName(),
            draft.getSaleDate(),
            draft.getSaleType(),
            draft.getVatMode(),
            draft.getVatPercent(),
            draft.getNote(),
            subTotal.setScale(PRICE_SCALE, RoundingMode.HALF_UP),
            vatAmount.setScale(PRICE_SCALE, RoundingMode.HALF_UP),
            normalizedTotal,
            draft.getPaymentMethod() == null ? PaymentMethod.CASH : draft.getPaymentMethod(),
            paidAmount,
            normalizedTotal.subtract(paidAmount).setScale(PRICE_SCALE, RoundingMode.HALF_UP),
            lines
        );
    }

    private SaleDraftSummary mapSummary(SaleDraftEntity draft) {
        SaleDraftResponse response = mapDraft(saleDraftRepository.findWithLinesById(draft.getId()).orElse(draft));
        return new SaleDraftSummary(
            draft.getId(),
            draft.getCustomer() == null ? "No customer selected" : draft.getCustomer().getName(),
            draft.getStore().getName(),
            response.lines().size(),
            response.total(),
            draft.getUpdatedAt()
        );
    }

    private LineAmounts calculateLineTotal(BigDecimal quantity, BigDecimal unitPrice, VatMode vatMode, BigDecimal vatPercent) {
        BigDecimal gross = quantity.multiply(unitPrice).setScale(PRICE_SCALE, RoundingMode.HALF_UP);
        if (vatMode == VatMode.INCLUSIVE) {
            BigDecimal divisor = BigDecimal.ONE.add(vatPercent.divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP));
            BigDecimal subTotal = gross.divide(divisor, PRICE_SCALE, RoundingMode.HALF_UP);
            return new LineAmounts(subTotal, gross.subtract(subTotal), gross);
        }
        BigDecimal vatAmount = gross.multiply(vatPercent).divide(BigDecimal.valueOf(100), PRICE_SCALE, RoundingMode.HALF_UP);
        return new LineAmounts(gross, vatAmount, gross.add(vatAmount));
    }

    private BigDecimal normalizeQuantity(BigDecimal quantity) {
        if (quantity == null) {
            throw new DuplicateResourceException("Quantity is required");
        }
        try {
            BigDecimal normalized = quantity.setScale(QUANTITY_SCALE, RoundingMode.UNNECESSARY);
            if (normalized.compareTo(BigDecimal.ZERO) <= 0) {
                throw new DuplicateResourceException("Quantity must be greater than zero");
            }
            return normalized;
        } catch (ArithmeticException ex) {
            throw new DuplicateResourceException("Quantity must be a whole number");
        }
    }

    private BigDecimal normalizePrice(BigDecimal price) {
        if (price == null) {
            throw new DuplicateResourceException("Price is required");
        }
        try {
            BigDecimal normalized = price.setScale(PRICE_SCALE, RoundingMode.UNNECESSARY);
            if (normalized.compareTo(BigDecimal.ZERO) < 0) {
                throw new DuplicateResourceException("Price cannot be negative");
            }
            return normalized;
        } catch (ArithmeticException ex) {
            throw new DuplicateResourceException("Price can have maximum 2 decimals");
        }
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

    private boolean contains(String value, String pattern) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(pattern);
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private ApplicationKeyword parseApplicationKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return new ApplicationKeyword(null, null);
        }

        Matcher matcher = YEAR_PATTERN.matcher(keyword);
        if (!matcher.find()) {
            return new ApplicationKeyword(null, null);
        }

        Integer year = Integer.valueOf(matcher.group(1));
        String text = matcher.replaceAll(" ").trim().replaceAll("\\s+", " ");
        if (text.isBlank()) {
            return new ApplicationKeyword(null, year);
        }

        return new ApplicationKeyword("%" + text.toLowerCase(Locale.ROOT) + "%", year);
    }

    private void recordAction(SaleDraftEntity draft, String actionType, String details) {
        SaleDraftActionEntity action = new SaleDraftActionEntity();
        action.setDraft(draft);
        action.setActionType(actionType);
        action.setDetails(details);
        saleDraftActionRepository.save(action);
    }

    private record LineAmounts(BigDecimal subTotal, BigDecimal vatAmount, BigDecimal total) {
    }

    private record ApplicationKeyword(String pattern, Integer year) {
    }
}
