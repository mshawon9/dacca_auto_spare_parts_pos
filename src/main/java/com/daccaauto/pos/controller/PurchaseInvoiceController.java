package com.daccaauto.pos.controller;

import com.daccaauto.pos.dto.category.ProductCategoryCreateRequest;
import com.daccaauto.pos.dto.category.ProductCategoryResponse;
import com.daccaauto.pos.dto.product.ProductCreateRequest;
import com.daccaauto.pos.dto.purchase.InvoiceExtraction;
import com.daccaauto.pos.entity.ProductPosition;
import com.daccaauto.pos.service.*;
import com.daccaauto.pos.service.PurchaseInvoiceMatchService.Query;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/purchase-orders/invoice")
@RequiredArgsConstructor
public class PurchaseInvoiceController {
    private final PurchaseInvoiceAiService ai;
    private final PurchaseInvoiceMatchService matches;
    private final PurchaseInvoiceProductService products;
    private final ProductCategoryService categories;
    private final BrandService brands;

    @PostMapping("/extract")
    public InvoiceExtraction extract(@RequestParam("file") MultipartFile file) { return ai.extract(file); }

    @PostMapping("/demo")
    public InvoiceExtraction demo() { return ai.demo(); }

    public record MatchBatch(@NotNull @Size(min = 1, max = 100) List<@NotNull @Valid Query> lines) { }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<?> extractionError(ResponseStatusException ex) {
        return ResponseEntity.status(ex.getStatusCode()).body(Map.of("error", ex.getReason() == null ? "Invoice request failed." : ex.getReason()));
    }

    @PostMapping("/match")
    public List<PurchaseInvoiceMatchService.Match> match(@Valid @RequestBody MatchBatch batch) {
        return batch.lines().stream().map(matches::match).toList();
    }

    @GetMapping("/references")
    public Map<String, Object> references() {
        return Map.of("categories", categories.getAll().stream().filter(ProductCategoryResponse::active).toList(),
            "brands", brands.getAll().stream().filter(b -> b.active()).toList(),
            "positions", Arrays.stream(ProductPosition.values()).map(p -> Map.of("value", p.name(), "text", p.getDisplayName())).toList());
    }

    @PostMapping("/categories")
    public ProductCategoryResponse category(@Valid @RequestBody ProductCategoryCreateRequest request) { return categories.create(request); }

    @PostMapping("/products")
    public PurchaseOrderController.ProductOption product(@Valid @RequestBody ProductCreateRequest request) {
        var product = products.create(request);
        return new PurchaseOrderController.ProductOption(product.id(), product.name() + " | Brand: " + product.brandName() + " | Part: " + product.partNumber());
    }
}
