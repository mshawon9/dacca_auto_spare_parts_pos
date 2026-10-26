package com.daccaauto.pos.dto.purchase;

import java.math.BigDecimal;
import java.util.List;

public record InvoiceExtraction(String supplierName, String invoiceId, String purchaseDate,
                                String currency, BigDecimal total, List<Line> lines, List<String> warnings) {
    public InvoiceExtraction {
        lines = List.copyOf(lines);
        warnings = List.copyOf(warnings);
    }

    public record Line(String name, String partNumber, String sku, String brand, String category,
                       String supplierProductCode, BigDecimal quantity, BigDecimal unitPrice,
                       BigDecimal taxPercent) { }
}
