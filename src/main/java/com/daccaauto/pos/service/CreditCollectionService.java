package com.daccaauto.pos.service;

import com.daccaauto.pos.dto.sale.CreditCollectionRequest;
import com.daccaauto.pos.dto.sale.CreditCollectionSummary;
import com.daccaauto.pos.dto.sale.CreditSaleRow;
import com.daccaauto.pos.dto.sale.MonthlyStatementCollectionSummary;
import com.daccaauto.pos.dto.sale.SalePaymentRow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

public interface CreditCollectionService {

    Page<CreditSaleRow> search(String keyword,
                               Long customerId,
                               String collectionType,
                               boolean overdueOnly,
                               Pageable pageable);

    CreditCollectionSummary summarize(String keyword,
                                      Long customerId,
                                      String collectionType,
                                      boolean overdueOnly);

    void collect(Long saleId, CreditCollectionRequest request);

    MonthlyStatementCollectionSummary monthlyStatementSummary(Long customerId, YearMonth statementMonth);

    void collectMonthlyStatement(Long customerId, YearMonth statementMonth, CreditCollectionRequest request);

    void updateDueDate(Long saleId, LocalDate dueDate);

    List<SalePaymentRow> payments(Long saleId);
}
