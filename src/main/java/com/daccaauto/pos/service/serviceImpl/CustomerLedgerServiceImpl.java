package com.daccaauto.pos.service.serviceImpl;

import com.daccaauto.pos.dto.sale.CreditCollectionRequest;
import com.daccaauto.pos.dto.sale.CustomerLedgerRow;
import com.daccaauto.pos.dto.sale.CustomerLedgerSummary;
import com.daccaauto.pos.entity.CustomerEntity;
import com.daccaauto.pos.entity.PaymentMethod;
import com.daccaauto.pos.entity.SaleEntity;
import com.daccaauto.pos.entity.SalePaymentEntity;
import com.daccaauto.pos.exception.DuplicateResourceException;
import com.daccaauto.pos.exception.ResourceNotFoundException;
import com.daccaauto.pos.repository.CustomerRepository;
import com.daccaauto.pos.repository.SalePaymentRepository;
import com.daccaauto.pos.repository.SaleRepository;
import com.daccaauto.pos.service.CustomerLedgerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CustomerLedgerServiceImpl implements CustomerLedgerService {

    private static final int PRICE_SCALE = 2;
    private static final LocalDate MIN_DATE = LocalDate.of(1900, 1, 1);
    private static final LocalDate MAX_DATE = LocalDate.of(9999, 12, 31);

    private final CustomerRepository customerRepository;
    private final SaleRepository saleRepository;
    private final SalePaymentRepository salePaymentRepository;

    @Override
    @Transactional(readOnly = true)
    public List<CustomerLedgerRow> ledger(Long customerId,
                                          LocalDate fromDate,
                                          LocalDate toDate,
                                          String ledgerType,
                                          YearMonth statementMonth) {
        if (customerId == null) {
            return List.of();
        }
        DateRange range = range(fromDate, toDate, ledgerType, statementMonth);
        LedgerFilter filter = filter(ledgerType);
        List<SaleEntity> sales = saleRepository.findCustomerSales(customerId, range.fromDate(), range.toDate())
            .stream()
            .filter(sale -> matches(filter, sale))
            .toList();
        List<SalePaymentEntity> payments = salePaymentRepository.findCustomerPayments(customerId, range.fromDate(), range.toDate())
            .stream()
            .filter(payment -> matches(filter, payment.getSale()))
            .toList();

        Map<Long, BigDecimal> paymentBySaleId = payments.stream()
            .collect(Collectors.groupingBy(
                payment -> payment.getSale().getId(),
                Collectors.reducing(BigDecimal.ZERO.setScale(PRICE_SCALE), payment -> money(payment.getAmount()), BigDecimal::add)
            ));
        List<LedgerEntry> entries = new ArrayList<>();
        for (SaleEntity sale : sales) {
            entries.add(LedgerEntry.sale(sale));
            BigDecimal historicalPaid = money(sale.getPaidAmount()).subtract(paymentBySaleId.getOrDefault(sale.getId(), BigDecimal.ZERO.setScale(PRICE_SCALE)));
            if (historicalPaid.compareTo(BigDecimal.ZERO) > 0) {
                entries.add(LedgerEntry.initialPayment(sale, historicalPaid));
            }
        }
        entries.addAll(groupPayments(payments));
        entries.sort(Comparator
            .comparing(LedgerEntry::entryDate)
            .thenComparingInt(LedgerEntry::sortOrder)
            .thenComparing(LedgerEntry::reference));

        BigDecimal balance = BigDecimal.ZERO.setScale(PRICE_SCALE);
        List<CustomerLedgerRow> rows = new ArrayList<>();
        for (LedgerEntry entry : entries) {
            balance = balance.add(entry.debit()).subtract(entry.credit()).setScale(PRICE_SCALE, RoundingMode.HALF_UP);
            rows.add(entry.toRow(balance));
        }
        return rows;
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerLedgerSummary summarize(Long customerId,
                                           LocalDate fromDate,
                                           LocalDate toDate,
                                           String ledgerType,
                                           YearMonth statementMonth) {
        if (customerId == null) {
            return new CustomerLedgerSummary(null, money(null), money(null), money(null));
        }
        CustomerEntity customer = customerRepository.findById(customerId)
            .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + customerId));
        List<CustomerLedgerRow> rows = ledger(customerId, fromDate, toDate, ledgerType, statementMonth);
        BigDecimal totalSale = rows.stream()
            .map(CustomerLedgerRow::debit)
            .reduce(BigDecimal.ZERO.setScale(PRICE_SCALE), BigDecimal::add);
        BigDecimal totalCollection = rows.stream()
            .map(CustomerLedgerRow::credit)
            .reduce(BigDecimal.ZERO.setScale(PRICE_SCALE), BigDecimal::add);
        BigDecimal balance = rows.isEmpty() ? BigDecimal.ZERO.setScale(PRICE_SCALE) : rows.get(rows.size() - 1).balance();
        return new CustomerLedgerSummary(customer.getName(), totalSale, totalCollection, balance);
    }

    @Override
    public void collectCustomerPayment(Long customerId,
                                       String ledgerType,
                                       YearMonth statementMonth,
                                       CreditCollectionRequest request) {
        if (customerId == null) {
            throw new DuplicateResourceException("Customer is required.");
        }
        BigDecimal amount = normalizeAmount(request.getAmount());
        LocalDate receiveDate = request.getReceiveDate() == null ? LocalDate.now() : request.getReceiveDate();
        PaymentMethod paymentMethod = request.getPaymentMethod() == null ? PaymentMethod.CASH : request.getPaymentMethod();
        if (paymentMethod == PaymentMethod.CHEQUE
            && (request.getChequeNumber() == null || request.getChequeNumber().isBlank())) {
            throw new DuplicateResourceException("Cheque number is required for cheque payment.");
        }

        DateRange range = collectionRange(ledgerType, statementMonth);
        LedgerFilter filter = filter(ledgerType);
        List<SaleEntity> sales = saleRepository.findCustomerCreditsForUpdate(
            customerId,
            range.fromDate(),
            range.toDate(),
            filter.monthlyOnly(),
            filter.regularOnly()
        );
        BigDecimal totalDue = sales.stream()
            .map(sale -> money(sale.getBalanceDue()))
            .reduce(BigDecimal.ZERO.setScale(PRICE_SCALE), BigDecimal::add);
        if (totalDue.compareTo(BigDecimal.ZERO) <= 0) {
            throw new DuplicateResourceException("No due balance found for this customer.");
        }
        if (amount.compareTo(totalDue) > 0) {
            throw new DuplicateResourceException("Payment amount cannot be greater than customer due balance.");
        }

        BigDecimal remaining = amount;
        String collectionReference = collectionReference();
        String note = collectionNote(ledgerType, statementMonth, request.getNote());
        for (SaleEntity sale : sales) {
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }
            BigDecimal saleDue = money(sale.getBalanceDue());
            BigDecimal applied = remaining.min(saleDue);
            savePayment(sale, applied, receiveDate, paymentMethod, request.getChequeDate(), request.getChequeNumber(), collectionReference, note);
            sale.setPaidAmount(money(sale.getPaidAmount()).add(applied).setScale(PRICE_SCALE, RoundingMode.HALF_UP));
            sale.setBalanceDue(saleDue.subtract(applied).max(BigDecimal.ZERO).setScale(PRICE_SCALE, RoundingMode.HALF_UP));
            if (request.getDueDate() != null) {
                sale.setDueDate(request.getDueDate());
            }
            saleRepository.save(sale);
            remaining = remaining.subtract(applied).setScale(PRICE_SCALE, RoundingMode.HALF_UP);
        }
    }

    private List<LedgerEntry> groupPayments(List<SalePaymentEntity> payments) {
        Map<String, List<SalePaymentEntity>> groups = new LinkedHashMap<>();
        for (SalePaymentEntity payment : payments) {
            String key = payment.getCollectionReference() == null || payment.getCollectionReference().isBlank()
                ? "PAY-" + payment.getId()
                : payment.getCollectionReference();
            groups.computeIfAbsent(key, ignored -> new ArrayList<>()).add(payment);
        }
        return groups.entrySet().stream()
            .map(entry -> LedgerEntry.payment(entry.getKey(), entry.getValue()))
            .toList();
    }

    private boolean matches(LedgerFilter filter, SaleEntity sale) {
        if (filter.monthlyOnly()) {
            return sale.getSaleType() == com.daccaauto.pos.entity.SaleType.MONTHLY_STATEMENT;
        }
        if (filter.regularOnly()) {
            return sale.getSaleType() != com.daccaauto.pos.entity.SaleType.MONTHLY_STATEMENT;
        }
        return true;
    }

    private LedgerFilter filter(String ledgerType) {
        String type = ledgerType == null ? "all" : ledgerType.toLowerCase(Locale.ROOT);
        return new LedgerFilter("monthly".equals(type), "regular".equals(type));
    }

    private DateRange range(LocalDate fromDate, LocalDate toDate, String ledgerType, YearMonth statementMonth) {
        if ("monthly".equalsIgnoreCase(ledgerType) && statementMonth != null) {
            return new DateRange(statementMonth.atDay(1), statementMonth.atEndOfMonth());
        }
        return new DateRange(fromDate == null ? MIN_DATE : fromDate, toDate == null ? MAX_DATE : toDate);
    }

    private DateRange collectionRange(String ledgerType, YearMonth statementMonth) {
        if ("monthly".equalsIgnoreCase(ledgerType)) {
            YearMonth month = statementMonth == null ? YearMonth.now() : statementMonth;
            return new DateRange(month.atDay(1), month.atEndOfMonth());
        }
        return new DateRange(MIN_DATE, MAX_DATE);
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

    private BigDecimal normalizeAmount(BigDecimal amount) {
        if (amount == null) {
            throw new DuplicateResourceException("Payment amount is required.");
        }
        try {
            BigDecimal normalized = amount.setScale(PRICE_SCALE, RoundingMode.UNNECESSARY);
            if (normalized.compareTo(BigDecimal.ZERO) <= 0) {
                throw new DuplicateResourceException("Payment amount must be greater than zero.");
            }
            return normalized;
        } catch (ArithmeticException ex) {
            throw new DuplicateResourceException("Payment amount can have maximum 2 decimals.");
        }
    }

    private BigDecimal money(BigDecimal value) {
        return value == null ? BigDecimal.ZERO.setScale(PRICE_SCALE) : value.setScale(PRICE_SCALE, RoundingMode.HALF_UP);
    }

    private String collectionReference() {
        return "COL-" + LocalDate.now() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
    }

    private String collectionNote(String ledgerType, YearMonth statementMonth, String note) {
        String trimmed = trimToNull(note);
        if ("monthly".equalsIgnoreCase(ledgerType)) {
            YearMonth month = statementMonth == null ? YearMonth.now() : statementMonth;
            return "Monthly customer payment " + month + (trimmed == null ? "" : " - " + trimmed);
        }
        return trimmed;
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private record DateRange(LocalDate fromDate, LocalDate toDate) {
    }

    private record LedgerFilter(boolean monthlyOnly, boolean regularOnly) {
    }

    private record LedgerEntry(LocalDate entryDate,
                               int sortOrder,
                               String entryType,
                               String reference,
                               String description,
                               com.daccaauto.pos.entity.SaleType saleType,
                               PaymentMethod paymentMethod,
                               LocalDate chequeDate,
                               String chequeNumber,
                               BigDecimal debit,
                               BigDecimal credit) {

        static LedgerEntry sale(SaleEntity sale) {
            return new LedgerEntry(
                sale.getSaleDate(),
                10,
                "SALE",
                sale.getInvoiceNo(),
                sale.getStore().getName(),
                sale.getSaleType(),
                null,
                null,
                null,
                sale.getTotal(),
                BigDecimal.ZERO.setScale(PRICE_SCALE)
            );
        }

        static LedgerEntry initialPayment(SaleEntity sale, BigDecimal amount) {
            return new LedgerEntry(
                sale.getSaleDate(),
                20,
                "PAID AT SALE",
                sale.getInvoiceNo(),
                "Paid when invoice was created",
                sale.getSaleType(),
                sale.getPaymentMethod(),
                null,
                null,
                BigDecimal.ZERO.setScale(PRICE_SCALE),
                amount
            );
        }

        static LedgerEntry payment(String reference, List<SalePaymentEntity> payments) {
            SalePaymentEntity first = payments.get(0);
            BigDecimal amount = payments.stream()
                .map(SalePaymentEntity::getAmount)
                .reduce(BigDecimal.ZERO.setScale(PRICE_SCALE), BigDecimal::add);
            String invoices = payments.stream()
                .map(payment -> payment.getSale().getInvoiceNo())
                .distinct()
                .collect(Collectors.joining(", "));
            return new LedgerEntry(
                first.getReceiveDate(),
                30,
                "PAYMENT",
                reference,
                invoices,
                first.getSale().getSaleType(),
                first.getPaymentMethod(),
                first.getChequeDate(),
                first.getChequeNumber(),
                BigDecimal.ZERO.setScale(PRICE_SCALE),
                amount
            );
        }

        CustomerLedgerRow toRow(BigDecimal balance) {
            return new CustomerLedgerRow(
                entryDate,
                entryType,
                reference,
                description,
                saleType,
                paymentMethod,
                chequeDate,
                chequeNumber,
                debit.setScale(PRICE_SCALE, RoundingMode.HALF_UP),
                credit.setScale(PRICE_SCALE, RoundingMode.HALF_UP),
                balance
            );
        }
    }
}
