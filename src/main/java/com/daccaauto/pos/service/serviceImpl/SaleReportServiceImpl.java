package com.daccaauto.pos.service.serviceImpl;

import com.daccaauto.pos.dto.sale.SaleDetail;
import com.daccaauto.pos.dto.sale.SaleHistoryRow;
import com.daccaauto.pos.dto.sale.SaleStatementSummary;
import com.daccaauto.pos.entity.SaleEntity;
import com.daccaauto.pos.exception.ResourceNotFoundException;
import com.daccaauto.pos.repository.SaleRepository;
import com.daccaauto.pos.service.SaleReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class SaleReportServiceImpl implements SaleReportService {

    private static final LocalDate MIN_REPORT_DATE = LocalDate.of(1900, 1, 1);
    private static final LocalDate MAX_REPORT_DATE = LocalDate.of(9999, 12, 31);

    private final SaleRepository saleRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<SaleHistoryRow> search(String keyword,
                                       LocalDate fromDate,
                                       LocalDate toDate,
                                       Long customerId,
                                       boolean creditOnly,
                                       Pageable pageable) {
        String pattern = keywordPattern(keyword);
        return saleRepository.searchSales(
                pattern == null ? "%%" : pattern,
                pattern != null,
                normalizeFromDate(fromDate),
                normalizeToDate(toDate),
                normalizeCustomerId(customerId),
                creditOnly,
                pageable
            )
            .map(this::mapRow);
    }

    @Override
    @Transactional(readOnly = true)
    public SaleDetail getDetail(Long id) {
        SaleEntity sale = saleRepository.findWithLinesById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Sale not found: " + id));
        return mapDetail(sale);
    }

    @Override
    @Transactional(readOnly = true)
    public SaleStatementSummary summarize(LocalDate fromDate,
                                          LocalDate toDate,
                                          Long customerId,
                                          boolean creditOnly) {
        Object[] raw = saleRepository.summarizeSales(
            normalizeFromDate(fromDate),
            normalizeToDate(toDate),
            normalizeCustomerId(customerId),
            creditOnly
        );
        Object[] values = raw != null && raw.length == 1 && raw[0] instanceof Object[] nested ? nested : raw;
        return new SaleStatementSummary(
            ((Number) values[5]).longValue(),
            asMoney(values[0]),
            asMoney(values[1]),
            asMoney(values[2]),
            asMoney(values[3]),
            asMoney(values[4])
        );
    }

    private SaleHistoryRow mapRow(SaleEntity sale) {
        return new SaleHistoryRow(
            sale.getId(),
            sale.getInvoiceNo(),
            sale.getSaleDate(),
            sale.getCustomer() == null ? "Walk-in customer" : sale.getCustomer().getName(),
            sale.getStore().getName(),
            sale.getSaleType(),
            sale.getVatMode(),
            sale.getPaymentMethod(),
            sale.getLines() == null ? 0 : sale.getLines().size(),
            sale.getSubTotal(),
            sale.getVatAmount(),
            sale.getTotal(),
            sale.getPaidAmount(),
            sale.getBalanceDue()
        );
    }

    private SaleDetail mapDetail(SaleEntity sale) {
        return new SaleDetail(
            sale.getId(),
            sale.getInvoiceNo(),
            sale.getSaleDate(),
            sale.getCustomer() == null ? "Walk-in customer" : sale.getCustomer().getName(),
            sale.getStore().getName(),
            sale.getSaleType(),
            sale.getVatMode(),
            sale.getVatPercent(),
            sale.getPaymentMethod(),
            sale.getSubTotal(),
            sale.getVatAmount(),
            sale.getTotal(),
            sale.getPaidAmount(),
            sale.getBalanceDue(),
            sale.getDueDate(),
            sale.getLines().stream()
                .map(line -> new SaleDetail.Line(
                    line.getId(),
                    line.getProduct().getId(),
                    line.getProduct().getName(),
                    line.getProduct().getCategory() == null ? null : line.getProduct().getCategory().getName(),
                    line.getProduct().getBrand() == null ? null : line.getProduct().getBrand().getName(),
                    line.getProduct().getPartNumber(),
                    line.getQuantity(),
                    line.getUnitPrice(),
                    line.getCostPrice(),
                    line.getLineTotal()
                ))
                .toList()
        );
    }

    private String keywordPattern(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        return "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";
    }

    private LocalDate normalizeFromDate(LocalDate fromDate) {
        return fromDate == null ? MIN_REPORT_DATE : fromDate;
    }

    private LocalDate normalizeToDate(LocalDate toDate) {
        return toDate == null ? MAX_REPORT_DATE : toDate;
    }

    private Long normalizeCustomerId(Long customerId) {
        return customerId == null ? 0L : customerId;
    }

    private BigDecimal asMoney(Object value) {
        return value instanceof BigDecimal money ? money : BigDecimal.ZERO;
    }
}
