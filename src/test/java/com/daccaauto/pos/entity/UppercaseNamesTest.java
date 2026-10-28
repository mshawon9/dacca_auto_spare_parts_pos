package com.daccaauto.pos.entity;

import org.junit.jupiter.api.Test;
import java.util.Locale;
import static org.junit.jupiter.api.Assertions.*;

class UppercaseNamesTest {
    @Test void normalizesAllMasterNamesWithoutChangingOtherFields() {
        var brand = new BrandEntity();
        brand.setName("  kyb  ");
        brand.setCode("code");
        assertEquals("KYB", brand.getName());
        assertEquals("code", brand.getCode());
        var category = new ProductCategoryEntity();
        category.setName("brake pad");
        assertEquals("BRAKE PAD", category.getName());
        var supplier = new SupplierEntity();
        supplier.setName(" Gwd L.L.C ");
        supplier.setEmail("sales@example.com");
        assertEquals("GWD L.L.C", supplier.getName());
        assertEquals("sales@example.com", supplier.getEmail());
        supplier.setName(null);
        assertNull(supplier.getName());
    }

    @Test void casingDoesNotDependOnServerLocale() {
        Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr"));
            var brand = new BrandEntity();
            brand.setName("aisin");
            assertEquals("AISIN", brand.getName());
        } finally {
            Locale.setDefault(previous);
        }
    }
}
