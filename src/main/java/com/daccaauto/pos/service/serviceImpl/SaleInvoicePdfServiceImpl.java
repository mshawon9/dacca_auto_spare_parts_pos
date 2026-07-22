package com.daccaauto.pos.service.serviceImpl;

import com.daccaauto.pos.dto.sale.SaleInvoicePdfLine;
import com.daccaauto.pos.entity.CustomerEntity;
import com.daccaauto.pos.entity.SaleEntity;
import com.daccaauto.pos.entity.SaleLineEntity;
import com.daccaauto.pos.entity.VatMode;
import com.daccaauto.pos.exception.ResourceNotFoundException;
import com.daccaauto.pos.repository.SaleRepository;
import com.daccaauto.pos.service.SaleInvoicePdfService;
import lombok.RequiredArgsConstructor;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.util.JRLoader;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class SaleInvoicePdfServiceImpl implements SaleInvoicePdfService {

    private static final int PRICE_SCALE = 2;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final SaleRepository saleRepository;

    @Override
    @Transactional(readOnly = true)
    public byte[] generate(Long saleId) {
        SaleEntity sale = saleRepository.findWithLinesById(saleId)
            .orElseThrow(() -> new ResourceNotFoundException("Sale not found: " + saleId));
        try {
            JasperReport report = loadReport();
            JasperPrint print = JasperFillManager.fillReport(
                report,
                parameters(sale),
                new JRBeanCollectionDataSource(lines(sale))
            );
            return JasperExportManager.exportReportToPdf(print);
        } catch (JRException | IOException ex) {
            throw new IllegalStateException("Could not generate invoice PDF", ex);
        }
    }

    private JasperReport loadReport() throws JRException, IOException {
        ClassPathResource compiled = new ClassPathResource("reports/dacca_invoice.jasper");
        if (compiled.exists()) {
            try (InputStream input = compiled.getInputStream()) {
                return (JasperReport) JRLoader.loadObject(input);
            }
        }
        try (InputStream input = new ClassPathResource("reports/dacca_invoice.jrxml").getInputStream()) {
            return JasperCompileManager.compileReport(input);
        }
    }

    private Map<String, Object> parameters(SaleEntity sale) {
        CustomerEntity customer = sale.getCustomer();
        Map<String, Object> params = new HashMap<>();
        params.put("P_CUSTOMER_NAME", customer == null ? "Walk-in customer" : customer.getName());
        params.put("P_CUSTOMER_MOBILE", customer == null ? null : customer.getPhone());
        params.put("P_CUSTOMER_ADDRESS", customer == null ? null : customer.getAddress());
        params.put("P_CUSTOMER_TRN", customer == null ? null : customer.getTrnNumber());
        params.put("P_INVOICE_NO", sale.getInvoiceNo());
        params.put("P_INVOICE_PAYMENT", sale.getPaymentMethod().name());
        params.put("P_INVOICE_DATE", sale.getSaleDate().format(DATE_FORMAT));
        params.put("P_INVOICE_NET_AMOUNT", money(sale.getSubTotal()));
        params.put("P_INVOICE_VAT", money(sale.getVatAmount()));
        params.put("P_INVOICE_TOTAL", money(sale.getTotal()));
        params.put("P_PAID_AMOUNT", money(sale.getPaidAmount()));
        params.put("P_BALANCE_DUE", money(sale.getBalanceDue()));
        params.put("P_DUE_DATE", sale.getDueDate() == null ? null : sale.getDueDate().format(DATE_FORMAT));
        params.put("P_TOTAL_IN_WORDS", amountInWords(sale.getTotal()));
        params.put("P_LPO", sale.getNote());
        params.put("REPORT_TITLE", "TAX INVOICE");
        return params;
    }

    private List<SaleInvoicePdfLine> lines(SaleEntity sale) {
        return IntStream.range(0, sale.getLines().size())
            .mapToObj(index -> mapLine(index + 1, sale, sale.getLines().get(index)))
            .toList();
    }

    private SaleInvoicePdfLine mapLine(int index, SaleEntity sale, SaleLineEntity line) {
        BigDecimal lineTotal = money(line.getLineTotal());
        BigDecimal subTotal = lineSubTotal(sale, line, lineTotal);
        return new SaleInvoicePdfLine(
            index,
            line.getProduct().getName(),
            line.getProduct().getPartNumber(),
            line.getQuantity(),
            money(line.getUnitPrice()),
            subTotal,
            sale.getVatPercent(),
            lineTotal.subtract(subTotal).setScale(PRICE_SCALE, RoundingMode.HALF_UP),
            lineTotal
        );
    }

    private BigDecimal lineSubTotal(SaleEntity sale, SaleLineEntity line, BigDecimal lineTotal) {
        if (sale.getVatMode() == VatMode.INCLUSIVE) {
            BigDecimal divisor = BigDecimal.ONE.add(sale.getVatPercent().divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP));
            return lineTotal.divide(divisor, PRICE_SCALE, RoundingMode.HALF_UP);
        }
        return line.getQuantity().multiply(line.getUnitPrice()).setScale(PRICE_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal money(BigDecimal value) {
        return value == null ? BigDecimal.ZERO.setScale(PRICE_SCALE) : value.setScale(PRICE_SCALE, RoundingMode.HALF_UP);
    }

    private String amountInWords(BigDecimal amount) {
        long dirhams = money(amount).longValue();
        int fils = money(amount).remainder(BigDecimal.ONE).movePointRight(2).intValue();
        String words = numberToWords(dirhams) + " Dirhams";
        if (fils > 0) {
            words += " and " + numberToWords(fils) + " Fils";
        }
        return words + " Only";
    }

    private String numberToWords(long number) {
        if (number == 0) {
            return "Zero";
        }
        String[] units = {"", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine", "Ten",
            "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen", "Seventeen", "Eighteen", "Nineteen"};
        String[] tens = {"", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety"};
        if (number < 20) {
            return units[(int) number];
        }
        if (number < 100) {
            return tens[(int) number / 10] + (number % 10 == 0 ? "" : " " + units[(int) number % 10]);
        }
        if (number < 1000) {
            return units[(int) number / 100] + " Hundred" + (number % 100 == 0 ? "" : " " + numberToWords(number % 100));
        }
        if (number < 1_000_000) {
            return numberToWords(number / 1000) + " Thousand" + (number % 1000 == 0 ? "" : " " + numberToWords(number % 1000));
        }
        return numberToWords(number / 1_000_000) + " Million" + (number % 1_000_000 == 0 ? "" : " " + numberToWords(number % 1_000_000));
    }
}
