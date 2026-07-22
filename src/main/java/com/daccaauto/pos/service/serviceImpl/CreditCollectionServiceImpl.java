package com.daccaauto.pos.service.serviceImpl;

import com.daccaauto.pos.dto.sale.CreditCollectionRequest;
import com.daccaauto.pos.dto.sale.CreditCollectionSummary;
import com.daccaauto.pos.dto.sale.CreditSaleRow;
import com.daccaauto.pos.dto.sale.MonthlyStatementCollectionSummary;
import com.daccaauto.pos.dto.sale.SalePaymentRow;
import com.daccaauto.pos.entity.CustomerEntity;
import com.daccaauto.pos.entity.PaymentMethod;
import com.daccaauto.pos.entity.SaleEntity;
import com.daccaauto.pos.entity.SalePaymentEntity;
import com.daccaauto.pos.exception.DuplicateResourceException;
import com.daccaauto.pos.exception.ResourceNotFoundException;
import com.daccaauto.pos.repository.CustomerRepository;
import com.daccaauto.pos.repository.SalePaymentRepository;
import com.daccaauto.pos.repository.SaleRepository;
import com.daccaauto.pos.service.CreditCollectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class CreditCollectionServiceImpl implements CreditCollectionService {

    private static final int PRICE_SCALE = 2;

    private final SaleRepository saleRepository;
    private final SalePaymentRepository salePaymentRepository;
    private final CustomerRepository customerRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<CreditSaleRow> search(String keyword,
                                      Long customerId,
                                      String collectionType,
                                      boolean overdueOnly,
                                      Pageable pageable) {
        SearchFilter filter = filter(keyword, customerId, collectionType, overdueOnly);
        LocalDate today = LocalDate.now();
        return saleRepository.searchOpenCredits(
                filter.keyword(),
                filter.hasKeyword(),
                filter.customerId(),
                filter.monthlyOnly(),
                filter.regularOnly(),
                filter.overdueOnly(),
                today,
                pageable
            )
            .map(sale -> mapSale(sale, today));
    }

    @Override
    @Transactional(readOnly = true)
    public CreditCollectionSummary summarize(String keyword,
                                             Long customerId,
                                             String collectionType,
                                             boolean overdueOnly) {
        SearchFilter filter = filter(keyword, customerId, collectionType, overdueOnly);
        Object[] raw = saleRepository.summarizeOpenCredits(
            filter.keyword(),
            filter.hasKeyword(),
            filter.customerId(),
            filter.monthlyOnly(),
            filter.regularOnly(),
            filter.overdueOnly(),
            LocalDate.now()
        );
        Object[] values = raw != null && raw.length == 1 && raw[0] instanceof Object[] nested ? nested : raw;
        return new CreditCollectionSummary(
            values == null ? 0L : ((Number) values[0]).longValue(),
            values == null ? BigDecimal.ZERO : asMoney(values[1]),
            values == null ? BigDecimal.ZERO : asMoney(values[2])
        );
    }

    @Override
    public void collect(Long saleId, CreditCollectionRequest request) {
        SaleEntity sale = saleRepository.findById(saleId)
            .orElseThrow(() -> new ResourceNotFoundException("Sale not found: " + saleId));
        BigDecimal balanceDue = money(sale.getBalanceDue());
        if (balanceDue.compareTo(BigDecimal.ZERO) <= 0) {
            throw new DuplicateResourceException("This invoice has no due balance.");
        }
        BigDecimal amount = normalizeAmount(request.getAmount());
        if (amount.compareTo(balanceDue) > 0) {
            throw new DuplicateResourceException("Collection amount cannot be greater than due balance.");
        }
        LocalDate receiveDate = request.getReceiveDate() == null ? LocalDate.now() : request.getReceiveDate();
        PaymentMethod paymentMethod = request.getPaymentMethod() == null ? PaymentMethod.CASH : request.getPaymentMethod();
        if (paymentMethod == PaymentMethod.CHEQUE
            && (request.getChequeNumber() == null || request.getChequeNumber().isBlank())) {
            throw new DuplicateResourceException("Cheque number is required for cheque payment.");
        }

        savePayment(sale, amount, receiveDate, paymentMethod, request.getChequeDate(), request.getChequeNumber(), collectionReference(), request.getNote());

        sale.setPaidAmount(money(sale.getPaidAmount()).add(amount).setScale(PRICE_SCALE, RoundingMode.HALF_UP));
        sale.setBalanceDue(balanceDue.subtract(amount).max(BigDecimal.ZERO).setScale(PRICE_SCALE, RoundingMode.HALF_UP));
        if (request.getDueDate() != null) {
            sale.setDueDate(request.getDueDate());
        }
        saleRepository.save(sale);
    }

    @Override
    @Transactional(readOnly = true)
    public MonthlyStatementCollectionSummary monthlyStatementSummary(Long customerId, YearMonth statementMonth) {
        if (customerId == null) {
            return emptyMonthlySummary(null, null, statementMonth);
        }
        CustomerEntity customer = customerRepository.findById(customerId)
            .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + customerId));
        YearMonth month = statementMonth == null ? YearMonth.now() : statementMonth;
        Object[] raw = saleRepository.summarizeMonthlyStatementCredit(customerId, month.atDay(1), month.atEndOfMonth());
        Object[] values = raw != null && raw.length == 1 && raw[0] instanceof Object[] nested ? nested : raw;
        if (values == null) {
            return emptyMonthlySummary(customer.getId(), customer.getName(), month);
        }
        return new MonthlyStatementCollectionSummary(
            customer.getId(),
            customer.getName(),
            month,
            ((Number) values[0]).longValue(),
            asMoney(values[1]),
            asMoney(values[2]),
            asMoney(values[3])
        );
    }

    @Override
    public void collectMonthlyStatement(Long customerId, YearMonth statementMonth, CreditCollectionRequest request) {
        if (customerId == null) {
            throw new DuplicateResourceException("Customer is required for monthly statement collection.");
        }
        YearMonth month = statementMonth == null ? YearMonth.now() : statementMonth;
        BigDecimal amount = normalizeAmount(request.getAmount());
        LocalDate receiveDate = request.getReceiveDate() == null ? LocalDate.now() : request.getReceiveDate();
        PaymentMethod paymentMethod = request.getPaymentMethod() == null ? PaymentMethod.CASH : request.getPaymentMethod();
        if (paymentMethod == PaymentMethod.CHEQUE
            && (request.getChequeNumber() == null || request.getChequeNumber().isBlank())) {
            throw new DuplicateResourceException("Cheque number is required for cheque payment.");
        }

        List<SaleEntity> monthlySales = saleRepository.findMonthlyStatementCreditsForUpdate(
            customerId,
            month.atDay(1),
            month.atEndOfMonth()
        );
        BigDecimal totalDue = monthlySales.stream()
            .map(sale -> money(sale.getBalanceDue()))
            .reduce(BigDecimal.ZERO.setScale(PRICE_SCALE), BigDecimal::add);
        if (totalDue.compareTo(BigDecimal.ZERO) <= 0) {
            throw new DuplicateResourceException("No monthly statement due found for this customer and month.");
        }
        if (amount.compareTo(totalDue) > 0) {
            throw new DuplicateResourceException("Collection amount cannot be greater than monthly statement due.");
        }

        BigDecimal remaining = amount;
        String collectionReference = collectionReference();
        String statementNote = "Monthly statement " + month + (trimToNull(request.getNote()) == null ? "" : " - " + trimToNull(request.getNote()));
        for (SaleEntity sale : monthlySales) {
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }
            BigDecimal saleDue = money(sale.getBalanceDue());
            BigDecimal applied = remaining.min(saleDue);
            savePayment(sale, applied, receiveDate, paymentMethod, request.getChequeDate(), request.getChequeNumber(), collectionReference, statementNote);
            sale.setPaidAmount(money(sale.getPaidAmount()).add(applied).setScale(PRICE_SCALE, RoundingMode.HALF_UP));
            sale.setBalanceDue(saleDue.subtract(applied).max(BigDecimal.ZERO).setScale(PRICE_SCALE, RoundingMode.HALF_UP));
            if (request.getDueDate() != null) {
                sale.setDueDate(request.getDueDate());
            }
            saleRepository.save(sale);
            remaining = remaining.subtract(applied).setScale(PRICE_SCALE, RoundingMode.HALF_UP);
        }
    }

    @Override
    public void updateDueDate(Long saleId, LocalDate dueDate) {
        SaleEntity sale = saleRepository.findById(saleId)
            .orElseThrow(() -> new ResourceNotFoundException("Sale not found: " + saleId));
        sale.setDueDate(dueDate);
        saleRepository.save(sale);
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.List<SalePaymentRow> payments(Long saleId) {
        return salePaymentRepository.findBySaleIdOrderByReceiveDateDescIdDesc(saleId)
            .stream()
            .map(payment -> new SalePaymentRow(
                payment.getId(),
                payment.getReceiveDate(),
                payment.getAmount(),
                payment.getPaymentMethod(),
                payment.getChequeDate(),
                payment.getChequeNumber(),
                payment.getNote()
            ))
            .toList();
    }

    private void savePayment(SaleEntity sale,
                             BigDecimal amount,
                             LocalDate receiveDate,
                             PaymentMethod paymentMethod,
                             LocalDate chequeDate,
                             String chequeNumber,
                             String collectionReference,
                             String note) {
        SalePaymentEntity payment = new SalePaymentEntity();
        payment.setSale(sale);
        payment.setReceiveDate(receiveDate);
        payment.setAmount(amount);
        payment.setPaymentMethod(paymentMethod);
        payment.setChequeDate(chequeDate);
        payment.setChequeNumber(trimToNull(chequeNumber));
        payment.setCollectionReference(collectionReference);
        payment.setNote(trimToNull(note));
        salePaymentRepository.save(payment);
    }

    private SearchFilter filter(String keyword, Long customerId, String collectionType, boolean overdueOnly) {
        String pattern = keyword == null || keyword.isBlank()
            ? "%%"
            : "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";
        String type = collectionType == null ? "all" : collectionType.toLowerCase(Locale.ROOT);
        return new SearchFilter(
            pattern,
            keyword != null && !keyword.isBlank(),
            customerId == null ? 0L : customerId,
            "monthly".equals(type),
            "regular".equals(type),
            overdueOnly
        );
    }

    private CreditSaleRow mapSale(SaleEntity sale, LocalDate today) {
        boolean overdue = sale.getDueDate() != null && sale.getDueDate().isBefore(today);
        return new CreditSaleRow(
            sale.getId(),
            sale.getInvoiceNo(),
            sale.getSaleDate(),
            sale.getCustomer() == null ? "Walk-in customer" : sale.getCustomer().getName(),
            sale.getStore().getName(),
            sale.getSaleType(),
            money(sale.getTotal()),
            money(sale.getPaidAmount()),
            money(sale.getBalanceDue()),
            sale.getDueDate(),
            overdue
        );
    }

    private BigDecimal normalizeAmount(BigDecimal amount) {
        if (amount == null) {
            throw new DuplicateResourceException("Collection amount is required.");
        }
        try {
            BigDecimal normalized = amount.setScale(PRICE_SCALE, RoundingMode.UNNECESSARY);
            if (normalized.compareTo(BigDecimal.ZERO) <= 0) {
                throw new DuplicateResourceException("Collection amount must be greater than zero.");
            }
            return normalized;
        } catch (ArithmeticException ex) {
            throw new DuplicateResourceException("Collection amount can have maximum 2 decimals.");
        }
    }

    private BigDecimal money(BigDecimal value) {
        return value == null ? BigDecimal.ZERO.setScale(PRICE_SCALE) : value.setScale(PRICE_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal asMoney(Object value) {
        return value instanceof BigDecimal money ? money.setScale(PRICE_SCALE, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(PRICE_SCALE);
    }

    private MonthlyStatementCollectionSummary emptyMonthlySummary(Long customerId, String customerName, YearMonth statementMonth) {
        YearMonth month = statementMonth == null ? YearMonth.now() : statementMonth;
        return new MonthlyStatementCollectionSummary(
            customerId,
            customerName,
            month,
            0L,
            BigDecimal.ZERO.setScale(PRICE_SCALE),
            BigDecimal.ZERO.setScale(PRICE_SCALE),
            BigDecimal.ZERO.setScale(PRICE_SCALE)
        );
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String collectionReference() {
        return "COL-" + LocalDate.now() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
    }

    private record SearchFilter(String keyword,
                                boolean hasKeyword,
                                Long customerId,
                                boolean monthlyOnly,
                                boolean regularOnly,
                                boolean overdueOnly) {
    }
}
