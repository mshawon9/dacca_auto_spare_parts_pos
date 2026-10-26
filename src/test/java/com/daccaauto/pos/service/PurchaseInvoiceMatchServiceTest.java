package com.daccaauto.pos.service;

import com.daccaauto.pos.entity.BrandEntity;
import com.daccaauto.pos.entity.ProductEntity;
import com.daccaauto.pos.repository.ProductRepository;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PurchaseInvoiceMatchServiceTest {
    private ProductEntity product(long id, String brand) {
        var entity = new ProductEntity();
        entity.setId(id); entity.setName("Brake Pad"); entity.setPartNumber("AB-123");
        var maker = new BrandEntity(); maker.setName(brand); entity.setBrand(maker);
        return entity;
    }

    private PurchaseInvoiceMatchService service(Map<String, List<ProductEntity>> results) {
        var repository = (ProductRepository) Proxy.newProxyInstance(ProductRepository.class.getClassLoader(),
            new Class<?>[]{ProductRepository.class}, (proxy, method, args) -> {
                if (method.getName().startsWith("matchInvoice")) return results.getOrDefault(method.getName(), List.of());
                throw new UnsupportedOperationException(method.getName());
            });
        return new PurchaseInvoiceMatchService(repository);
    }

    @Test void normalizesOnlyPartSeparatorsAndPreservesLeadingZeros() {
        assertEquals("001ABC", PurchaseInvoiceMatchService.normalizePart(" 00-1_ab.c/ "));
    }

    @Test void uniquePartNumberIsMatched() {
        var match = service(Map.of("matchInvoicePart", List.of(product(1, "KYB"))))
            .match(new PurchaseInvoiceMatchService.Query("Brake Pad", "AB-123", null, "KYB"));
        assertEquals("MATCHED", match.status()); assertEquals(1L, match.candidates().getFirst().id());
    }

    @Test void differentBrandsWithSamePartNeedCashierChoice() {
        var match = service(Map.of("matchInvoicePart", List.of(product(1, "KYB"), product(2, "BOSCH"))))
            .match(new PurchaseInvoiceMatchService.Query(null, "AB-123", null, null));
        assertEquals("CHOOSE_PRODUCT", match.status()); assertEquals(2, match.candidates().size());
    }

    @Test void nameAloneAlwaysRequiresConfirmation() {
        var match = service(Map.of("matchInvoiceName", List.of(product(1, "KYB"))))
            .match(new PurchaseInvoiceMatchService.Query("Brake Pad", null, null, null));
        assertEquals("CHOOSE_PRODUCT", match.status());
    }

    @Test void conflictingIdentifiersAreNotAutomaticallySelected() {
        var match = service(Map.of("matchInvoicePart", List.of(product(1, "KYB")), "matchInvoiceSku", List.of(product(2, "KYB"))))
            .match(new PurchaseInvoiceMatchService.Query(null, "AB-123", "SKU-2", null));
        assertEquals("CHOOSE_PRODUCT", match.status());
        var skuOnly = service(Map.of("matchInvoiceSku", List.of(product(2, "KYB"))))
            .match(new PurchaseInvoiceMatchService.Query(null, "unknown-part", "SKU-2", null));
        assertEquals("CHOOSE_PRODUCT", skuOnly.status());
    }

    @Test void skuWithoutPartCanMatchButUnknownAndWrongBrandsCannot() {
        var finder = service(Map.of("matchInvoiceSku", List.of(product(1, "KYB"))));
        assertEquals("MATCHED", finder.match(new PurchaseInvoiceMatchService.Query(null, null, "SKU", null)).status());
        assertEquals("NOT_FOUND", finder.match(new PurchaseInvoiceMatchService.Query(null, null, "SKU", "BOSCH")).status());
        assertEquals("NOT_FOUND", service(Map.of()).match(new PurchaseInvoiceMatchService.Query(null, null, null, null)).status());
    }
}
