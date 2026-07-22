package com.daccaauto.pos.service;

import com.daccaauto.pos.dto.sale.CreditCollectionRequest;
import com.daccaauto.pos.dto.sale.CustomerLedgerRow;
import com.daccaauto.pos.dto.sale.CustomerLedgerSummary;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

public interface CustomerLedgerService {

    List<CustomerLedgerRow> ledger(Long customerId,
                                   LocalDate fromDate,
                                   LocalDate toDate,
                                   String ledgerType,
                                   YearMonth statementMonth);

    CustomerLedgerSummary summarize(Long customerId,
                                    LocalDate fromDate,
                                    LocalDate toDate,
                                    String ledgerType,
                                    YearMonth statementMonth);

    void collectCustomerPayment(Long customerId,
                                String ledgerType,
                                YearMonth statementMonth,
                                CreditCollectionRequest request);
}
