package com.daccaauto.pos.service;

import com.daccaauto.pos.dto.sale.SaleDetail;
import com.daccaauto.pos.dto.sale.SaleHistoryRow;
import com.daccaauto.pos.dto.sale.SaleStatementSummary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface SaleReportService {

    Page<SaleHistoryRow> search(String keyword,
                                LocalDate fromDate,
                                LocalDate toDate,
                                Long customerId,
                                boolean creditOnly,
                                Pageable pageable);

    SaleDetail getDetail(Long id);

    SaleStatementSummary summarize(LocalDate fromDate,
                                   LocalDate toDate,
                                   Long customerId,
                                   boolean creditOnly);
}
