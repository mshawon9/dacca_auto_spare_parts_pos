package com.daccaauto.pos.service;

public interface SaleInvoicePdfService {

    byte[] generate(Long saleId);
}
