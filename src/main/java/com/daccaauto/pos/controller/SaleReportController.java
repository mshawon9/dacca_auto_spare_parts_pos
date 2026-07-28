package com.daccaauto.pos.controller;

import com.daccaauto.pos.dto.sale.SaleHistoryRow;
import com.daccaauto.pos.dto.sale.SaleStatementSummary;
import com.daccaauto.pos.entity.CustomerEntity;
import com.daccaauto.pos.repository.CustomerRepository;
import com.daccaauto.pos.service.ReportPdfService;
import com.daccaauto.pos.service.SaleInvoicePdfService;
import com.daccaauto.pos.service.SaleReportService;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/sales")
@RequiredArgsConstructor
public class SaleReportController {

    private static final int HISTORY_PAGE_SIZE = 15;
    private static final int STATEMENT_PAGE_SIZE = 25;

    private final SaleReportService saleReportService;
    private final SaleInvoicePdfService saleInvoicePdfService;
    private final ReportPdfService reportPdfService;
    private final CustomerRepository customerRepository;

    @GetMapping("/history")
    public String history(@RequestParam(required = false) String keyword,
                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
                          @RequestParam(required = false) Long customerId,
                          @RequestParam(defaultValue = "false") boolean creditOnly,
                          @RequestParam(defaultValue = "0") int page,
                          Model model) {
        Page<SaleHistoryRow> salePage = saleReportService.search(
            keyword,
            fromDate,
            toDate,
            customerId,
            creditOnly,
            PageRequest.of(Math.max(page, 0), HISTORY_PAGE_SIZE, Sort.by(Sort.Direction.DESC, "saleDate", "id"))
        );
        addCommonReportAttributes(model, salePage, keyword, fromDate, toDate, customerId, creditOnly);
        model.addAttribute("pageTitle", "Sale History");
        return "sale/history";
    }

    @GetMapping("/statements")
    public String statements(@RequestParam(defaultValue = "daily") String statementType,
                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                             @RequestParam(required = false) String month,
                             @RequestParam(required = false) Long customerId,
                             @RequestParam(defaultValue = "0") int page,
                             Model model) {
        LocalDate today = LocalDate.now();
        StatementRange range = resolveRange(statementType, date, month, today);
        boolean creditOnly = "credit".equalsIgnoreCase(statementType);

        Page<SaleHistoryRow> salePage = saleReportService.search(
            null,
            range.fromDate(),
            range.toDate(),
            customerId,
            creditOnly,
            PageRequest.of(Math.max(page, 0), STATEMENT_PAGE_SIZE, Sort.by(Sort.Direction.DESC, "saleDate", "id"))
        );

        model.addAttribute("salePage", salePage);
        model.addAttribute("sales", salePage.getContent());
        model.addAttribute("summary", saleReportService.summarize(range.fromDate(), range.toDate(), customerId, creditOnly));
        model.addAttribute("statementType", statementType);
        model.addAttribute("date", range.selectedDate());
        model.addAttribute("month", range.selectedMonth());
        model.addAttribute("fromDate", range.fromDate());
        model.addAttribute("toDate", range.toDate());
        model.addAttribute("customerId", customerId);
        model.addAttribute("creditOnly", creditOnly);
        model.addAttribute("customers", customerRepository.findAll(Sort.by(Sort.Direction.ASC, "name")));
        model.addAttribute("pageTitle", "Sale Statements");
        return "sale/statements";
    }

    @GetMapping("/{id}/detail-json")
    @ResponseBody
    public Object detailJson(@PathVariable Long id) {
        return saleReportService.getDetail(id);
    }

    @GetMapping("/{id}/invoice.pdf")
    public ResponseEntity<byte[]> invoicePdf(@PathVariable Long id) {
        String invoiceNo = saleReportService.getDetail(id).invoiceNo();
        byte[] bytes = saleInvoicePdfService.generate(id);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"invoice-" + invoiceNo + ".pdf\"")
            .contentType(MediaType.APPLICATION_PDF)
            .body(bytes);
    }

    @GetMapping("/statements/export.xlsx")
    public ResponseEntity<byte[]> exportStatementExcel(@RequestParam(defaultValue = "daily") String statementType,
                                                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                                       @RequestParam(required = false) String month,
                                                       @RequestParam(required = false) Long customerId) throws IOException {
        StatementRange range = resolveRange(statementType, date, month, LocalDate.now());
        boolean creditOnly = "credit".equalsIgnoreCase(statementType);
        List<SaleHistoryRow> sales = saleReportService.search(
                null,
                range.fromDate(),
                range.toDate(),
                customerId,
                creditOnly,
                PageRequest.of(0, 10000, Sort.by(Sort.Direction.DESC, "saleDate", "id"))
            )
            .getContent();
        SaleStatementSummary summary = saleReportService.summarize(range.fromDate(), range.toDate(), customerId, creditOnly);
        String customerName = customerId == null ? "All customers" : customerRepository.findById(customerId)
            .map(CustomerEntity::getName)
            .orElse("Customer #" + customerId);
        byte[] bytes = buildStatementWorkbook(statementType, range, customerName, sales, summary);
        String filename = "sale-statement-" + statementType.toLowerCase() + "-" + LocalDate.now() + ".xlsx";
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
            .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .body(bytes);
    }

    @GetMapping("/statements/export.pdf")
    public ResponseEntity<byte[]> exportStatementPdf(@RequestParam(defaultValue = "daily") String statementType,
                                                     @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                                     @RequestParam(required = false) String month,
                                                     @RequestParam(required = false) Long customerId) {
        StatementRange range = resolveRange(statementType, date, month, LocalDate.now());
        boolean creditOnly = "credit".equalsIgnoreCase(statementType);
        List<SaleHistoryRow> sales = saleReportService.search(
                null,
                range.fromDate(),
                range.toDate(),
                customerId,
                creditOnly,
                PageRequest.of(0, 10000, Sort.by(Sort.Direction.ASC, "saleDate", "id"))
            )
            .getContent();
        SaleStatementSummary summary = saleReportService.summarize(range.fromDate(), range.toDate(), customerId, creditOnly);
        String customerName = customerId == null ? "All customers" : customerRepository.findById(customerId)
            .map(CustomerEntity::getName)
            .orElse("Customer #" + customerId);
        String reportName = "daily".equalsIgnoreCase(statementType) ? "daily-sales" : "monthly-statement";
        String title = "daily".equalsIgnoreCase(statementType) ? "Daily Sales Report" : "Monthly Statement";
        byte[] bytes = reportPdfService.generate(reportName, saleReportParameters(title, statementType, range, customerName, summary), saleReportRows(sales));
        String filename = reportName + "-" + LocalDate.now() + ".pdf";
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
            .contentType(MediaType.APPLICATION_PDF)
            .body(bytes);
    }

    @GetMapping("/history/export.xlsx")
    public ResponseEntity<byte[]> exportHistoryExcel(@RequestParam(required = false) String keyword,
                                                     @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
                                                     @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
                                                     @RequestParam(required = false) Long customerId,
                                                     @RequestParam(defaultValue = "false") boolean creditOnly) throws IOException {
        List<SaleHistoryRow> sales = saleReportService.search(
                keyword,
                fromDate,
                toDate,
                customerId,
                creditOnly,
                PageRequest.of(0, 10000, Sort.by(Sort.Direction.DESC, "saleDate", "id"))
            )
            .getContent();
        SaleStatementSummary summary = saleReportService.summarize(fromDate, toDate, customerId, creditOnly);
        String customerName = customerId == null ? "All customers" : customerRepository.findById(customerId)
            .map(CustomerEntity::getName)
            .orElse("Customer #" + customerId);
        StatementRange range = new StatementRange(fromDate, toDate, LocalDate.now(), YearMonth.from(LocalDate.now()).toString());
        byte[] bytes = buildStatementWorkbook("history", range, customerName, sales, summary);
        String filename = "sale-history-" + LocalDate.now() + ".xlsx";
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
            .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .body(bytes);
    }

    private void addCommonReportAttributes(Model model,
                                           Page<SaleHistoryRow> salePage,
                                           String keyword,
                                           LocalDate fromDate,
                                           LocalDate toDate,
                                           Long customerId,
                                           boolean creditOnly) {
        model.addAttribute("salePage", salePage);
        model.addAttribute("sales", salePage.getContent());
        model.addAttribute("summary", saleReportService.summarize(fromDate, toDate, customerId, creditOnly));
        model.addAttribute("keyword", keyword);
        model.addAttribute("fromDate", fromDate);
        model.addAttribute("toDate", toDate);
        model.addAttribute("customerId", customerId);
        model.addAttribute("creditOnly", creditOnly);
        model.addAttribute("customers", customerRepository.findAll(Sort.by(Sort.Direction.ASC, "name")));
    }

    private StatementRange resolveRange(String statementType, LocalDate date, String month, LocalDate today) {
        if ("monthly".equalsIgnoreCase(statementType)) {
            YearMonth selectedMonth = month == null || month.isBlank() ? YearMonth.from(today) : YearMonth.parse(month);
            return new StatementRange(
                selectedMonth.atDay(1),
                selectedMonth.atEndOfMonth(),
                today,
                selectedMonth.toString()
            );
        }
        if ("all".equalsIgnoreCase(statementType) || "credit".equalsIgnoreCase(statementType)) {
            return new StatementRange(null, null, today, YearMonth.from(today).toString());
        }
        LocalDate selectedDate = date == null ? today : date;
        return new StatementRange(selectedDate, selectedDate, selectedDate, YearMonth.from(selectedDate).toString());
    }

    private record StatementRange(LocalDate fromDate, LocalDate toDate, LocalDate selectedDate, String selectedMonth) {
    }

    private byte[] buildStatementWorkbook(String statementType,
                                          StatementRange range,
                                          String customerName,
                                          List<SaleHistoryRow> sales,
                                          SaleStatementSummary summary) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Sale Statement");

            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);
            CellStyle titleStyle = workbook.createCellStyle();
            titleStyle.setFont(titleFont);

            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFont(headerFont);

            int rowIndex = 0;
            Row titleRow = sheet.createRow(rowIndex++);
            titleRow.createCell(0).setCellValue("Sale Statement");
            titleRow.getCell(0).setCellStyle(titleStyle);

            sheet.createRow(rowIndex++).createCell(0).setCellValue("Type: " + statementType);
            sheet.createRow(rowIndex++).createCell(0).setCellValue("Date Range: " + displayRange(range));
            sheet.createRow(rowIndex++).createCell(0).setCellValue("Customer: " + customerName);
            rowIndex++;

            Row header = sheet.createRow(rowIndex++);
            String[] headers = {"Invoice", "Date", "Customer", "Type", "Pay By", "Subtotal", "VAT", "Total", "Paid", "Balance"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            for (SaleHistoryRow sale : sales) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(sale.invoiceNo());
                row.createCell(1).setCellValue(sale.saleDate() == null ? "" : sale.saleDate().toString());
                row.createCell(2).setCellValue(sale.customerName());
                row.createCell(3).setCellValue(sale.saleType().name());
                row.createCell(4).setCellValue(sale.paymentMethod().name());
                row.createCell(5).setCellValue(toDouble(sale.subTotal()));
                row.createCell(6).setCellValue(toDouble(sale.vatAmount()));
                row.createCell(7).setCellValue(toDouble(sale.total()));
                row.createCell(8).setCellValue(toDouble(sale.paidAmount()));
                row.createCell(9).setCellValue(toDouble(sale.balanceDue()));
            }

            rowIndex++;
            Row totalRow = sheet.createRow(rowIndex);
            totalRow.createCell(4).setCellValue("Summary");
            totalRow.createCell(5).setCellValue(toDouble(summary.subTotal()));
            totalRow.createCell(6).setCellValue(toDouble(summary.vatAmount()));
            totalRow.createCell(7).setCellValue(toDouble(summary.total()));
            totalRow.createCell(8).setCellValue(toDouble(summary.paidAmount()));
            totalRow.createCell(9).setCellValue(toDouble(summary.balanceDue()));
            for (int i = 4; i <= 9; i++) {
                totalRow.getCell(i).setCellStyle(headerStyle);
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }
            workbook.write(output);
            return output.toByteArray();
        }
    }

    private String displayRange(StatementRange range) {
        if (range.fromDate() == null && range.toDate() == null) {
            return "All dates";
        }
        if (range.fromDate() != null && range.fromDate().equals(range.toDate())) {
            return range.fromDate().toString();
        }
        return range.fromDate() + " to " + range.toDate();
    }

    private double toDouble(BigDecimal value) {
        return value == null ? 0D : value.doubleValue();
    }

    private Map<String, Object> saleReportParameters(String title,
                                                     String statementType,
                                                     StatementRange range,
                                                     String customerName,
                                                     SaleStatementSummary summary) {
        Map<String, Object> params = new HashMap<>();
        params.put("REPORT_TITLE", title);
        params.put("P_STATEMENT_TYPE", statementType);
        params.put("P_DATE_RANGE", displayRange(range));
        params.put("P_CUSTOMER_NAME", customerName);
        params.put("P_INVOICE_COUNT", summary.invoiceCount());
        params.put("P_SUB_TOTAL", summary.subTotal());
        params.put("P_VAT_AMOUNT", summary.vatAmount());
        params.put("P_TOTAL", summary.total());
        params.put("P_PAID_AMOUNT", summary.paidAmount());
        params.put("P_BALANCE_DUE", summary.balanceDue());
        return params;
    }

    private List<Map<String, Object>> saleReportRows(List<SaleHistoryRow> sales) {
        return sales.stream()
            .map(sale -> {
                Map<String, Object> row = new HashMap<>();
                row.put("invoiceNo", sale.invoiceNo());
                row.put("saleDate", sale.saleDate() == null ? "" : sale.saleDate().toString());
                row.put("customerName", sale.customerName());
                row.put("paymentMethod", sale.paymentMethod() == null ? "" : sale.paymentMethod().name());
                row.put("subTotal", sale.subTotal());
                row.put("vatAmount", sale.vatAmount());
                row.put("total", sale.total());
                row.put("paidAmount", sale.paidAmount());
                row.put("balanceDue", sale.balanceDue());
                return row;
            })
            .toList();
    }
}
