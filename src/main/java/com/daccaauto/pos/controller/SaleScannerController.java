package com.daccaauto.pos.controller;

import com.daccaauto.pos.dto.sale.SaleProductOption;
import com.daccaauto.pos.repository.ProductRepository;
import com.daccaauto.pos.service.SaleScannerService;
import com.daccaauto.pos.service.SaleService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@Controller
@RequestMapping("/sales/scanner")
@RequiredArgsConstructor
public class SaleScannerController {
    private final SaleScannerService scanner;
    private final ProductRepository products;
    private final SaleService sales;

    @GetMapping("/mobile")
    public String mobile() { return "sale/scanner-mobile"; }

    @GetMapping("/product")
    @ResponseBody
    public SaleProductOption product(@RequestParam String code, @RequestParam Long storeId,
                                     @RequestParam(required = false) Long customerId) {
        if (code.isBlank() || code.length() > 200) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid barcode.");
        }
        var product = products.findByBarcodeAndActiveTrue(code.strip())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No active product has this barcode."));
        return sales.productInfo(product.getId(), storeId, customerId);
    }

    @PostMapping("/connections")
    @ResponseBody
    public SaleScannerService.Connection connect(HttpSession session) {
        return scanner.create(session.getId());
    }

    @GetMapping("/connections/{token}")
    @ResponseBody
    public SaleScannerService.Scan poll(@PathVariable String token, @RequestParam(defaultValue = "0") long acknowledged,
                                        HttpSession session) {
        return scanner.poll(token, session.getId(), acknowledged);
    }

    public record ScanRequest(String code) { }

    @PostMapping("/connections/{token}/scans")
    @ResponseBody
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void scan(@PathVariable String token, @RequestBody ScanRequest request) {
        scanner.submit(token, request.code());
    }

    @DeleteMapping("/connections/{token}")
    @ResponseBody
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void close(@PathVariable String token, HttpSession session) {
        scanner.close(token, session.getId());
    }
}
