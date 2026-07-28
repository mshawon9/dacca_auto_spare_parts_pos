package com.daccaauto.pos.controller;

import com.daccaauto.pos.dto.sale.CreditCollectionRequest;
import com.daccaauto.pos.dto.sale.CustomerLedgerRow;
import com.daccaauto.pos.dto.sale.CustomerLedgerSummary;
import com.daccaauto.pos.entity.PaymentMethod;
import com.daccaauto.pos.exception.DuplicateResourceException;
import com.daccaauto.pos.repository.CustomerRepository;
import com.daccaauto.pos.service.CustomerLedgerService;
import com.daccaauto.pos.service.ReportPdfService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/customer-ledgers")
@RequiredArgsConstructor
public class CustomerLedgerController {

    private final CustomerLedgerService customerLedgerService;
    private final ReportPdfService reportPdfService;
    private final CustomerRepository customerRepository;

    @GetMapping
    public String ledger(@RequestParam(required = false) Long customerId,
                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
                         @RequestParam(defaultValue = "all") String ledgerType,
                         @RequestParam(required = false) String statementMonth,
                         Model model) {
        YearMonth selectedMonth = parseMonth(statementMonth);
        model.addAttribute("customerId", customerId);
        model.addAttribute("fromDate", fromDate);
        model.addAttribute("toDate", toDate);
        model.addAttribute("ledgerType", ledgerType);
        model.addAttribute("statementMonth", selectedMonth.toString());
        model.addAttribute("customers", customerRepository.findAll(Sort.by(Sort.Direction.ASC, "name")));
        model.addAttribute("paymentMethods", Arrays.stream(PaymentMethod.values())
            .filter(method -> method != PaymentMethod.CREDIT)
            .toList());
        model.addAttribute("ledgerRows", customerLedgerService.ledger(customerId, fromDate, toDate, ledgerType, selectedMonth));
        model.addAttribute("summary", customerLedgerService.summarize(customerId, fromDate, toDate, ledgerType, selectedMonth));
        model.addAttribute("today", LocalDate.now());
        model.addAttribute("pageTitle", "Customer Ledger");
        return "customer-ledger/list";
    }

    @GetMapping("/export.pdf")
    public ResponseEntity<byte[]> exportPdf(@RequestParam Long customerId,
                                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
                                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
                                            @RequestParam(defaultValue = "all") String ledgerType,
                                            @RequestParam(required = false) String statementMonth) {
        YearMonth selectedMonth = parseMonth(statementMonth);
        List<CustomerLedgerRow> rows = customerLedgerService.ledger(customerId, fromDate, toDate, ledgerType, selectedMonth);
        CustomerLedgerSummary summary = customerLedgerService.summarize(customerId, fromDate, toDate, ledgerType, selectedMonth);
        byte[] bytes = reportPdfService.generate("customer-ledger", ledgerParameters(summary, fromDate, toDate, ledgerType, selectedMonth), ledgerRows(rows));
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"customer-ledger-" + customerId + ".pdf\"")
            .contentType(MediaType.APPLICATION_PDF)
            .body(bytes);
    }

    @PostMapping("/collect")
    public String collect(@RequestParam Long customerId,
                          @RequestParam(defaultValue = "all") String ledgerType,
                          @RequestParam(required = false) String statementMonth,
                          @Valid @ModelAttribute CreditCollectionRequest request,
                          BindingResult bindingResult,
                          RedirectAttributes redirectAttributes) {
        String redirectUrl = "redirect:/customer-ledgers?customerId=" + customerId
            + "&ledgerType=" + ledgerType
            + "&statementMonth=" + (statementMonth == null ? "" : statementMonth);
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please check payment amount and details.");
            return redirectUrl;
        }
        try {
            customerLedgerService.collectCustomerPayment(customerId, ledgerType, parseMonth(statementMonth), request);
            redirectAttributes.addFlashAttribute("successMessage", "Customer payment received successfully.");
        } catch (DuplicateResourceException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return redirectUrl;
    }

    private YearMonth parseMonth(String statementMonth) {
        return statementMonth == null || statementMonth.isBlank() ? YearMonth.now() : YearMonth.parse(statementMonth);
    }

    private Map<String, Object> ledgerParameters(CustomerLedgerSummary summary,
                                                 LocalDate fromDate,
                                                 LocalDate toDate,
                                                 String ledgerType,
                                                 YearMonth statementMonth) {
        Map<String, Object> params = new HashMap<>();
        params.put("REPORT_TITLE", "Customer Ledger");
        params.put("P_CUSTOMER_NAME", summary.customerName());
        params.put("P_LEDGER_TYPE", ledgerType);
        params.put("P_DATE_RANGE", "monthly".equalsIgnoreCase(ledgerType)
            ? statementMonth.toString()
            : ((fromDate == null ? "All" : fromDate.toString()) + " to " + (toDate == null ? "All" : toDate.toString())));
        params.put("P_TOTAL_SALE", summary.totalSale());
        params.put("P_TOTAL_COLLECTION", summary.totalCollection());
        params.put("P_BALANCE_DUE", summary.balanceDue());
        return params;
    }

    private List<Map<String, Object>> ledgerRows(List<CustomerLedgerRow> rows) {
        return rows.stream()
            .map(row -> {
                Map<String, Object> map = new HashMap<>();
                map.put("entryDate", row.entryDate() == null ? "" : row.entryDate().toString());
                map.put("entryType", row.entryType());
                map.put("reference", row.reference());
                map.put("description", row.description());
                map.put("paymentMethod", row.paymentMethod() == null ? "" : row.paymentMethod().name());
                map.put("cheque", row.chequeNumber() == null ? "" : row.chequeNumber() + (row.chequeDate() == null ? "" : " / " + row.chequeDate()));
                map.put("debit", row.debit());
                map.put("credit", row.credit());
                map.put("balance", row.balance());
                return map;
            })
            .toList();
    }
}
