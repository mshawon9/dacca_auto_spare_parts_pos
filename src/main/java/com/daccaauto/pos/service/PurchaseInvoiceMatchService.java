package com.daccaauto.pos.service;

import com.daccaauto.pos.entity.ProductEntity;
import com.daccaauto.pos.repository.ProductRepository;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PurchaseInvoiceMatchService {
    private final ProductRepository products;
    public record Query(@Size(max = 200) String name, @Size(max = 100) String partNumber,
                        @Size(max = 100) String sku, @Size(max = 100) String brand) { }
    public record Candidate(Long id, String text, String reason) { }
    public record Match(String status, List<Candidate> candidates) { }

    public Match match(Query query) {
        var page = PageRequest.of(0, 21);
        var exact = new LinkedHashMap<Long, Candidate>();
        String part = normalizePart(query.partNumber());
        String brand = query.brand() == null || query.brand().isBlank() ? null : query.brand().strip().toLowerCase(Locale.ROOT);
        if (!part.isEmpty()) {
            add(exact, products.matchInvoicePart(part, brand, page), query.brand(), "Part number");
        }
        if (query.sku() != null && !query.sku().isBlank()) {
            add(exact, products.matchInvoiceSku(query.sku().strip().toLowerCase(Locale.ROOT), brand, page), query.brand(), "SKU");
        }
        if (!exact.isEmpty()) {
            boolean clearMatch = exact.size() == 1 && (part.isEmpty() || exact.values().iterator().next().reason().equals("Part number"));
            return new Match(clearMatch ? "MATCHED" : "CHOOSE_PRODUCT", List.copyOf(exact.values()));
        }
        var names = new LinkedHashMap<Long, Candidate>();
        if (query.name() != null && !query.name().isBlank()) {
            String name = query.name().strip().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
            add(names, products.matchInvoiceName(name, brand, page), query.brand(), "Name (category prefix ignored) - confirm product");
            if (names.isEmpty()) {
                String pattern = descriptionPattern(name);
                if (pattern != null) {
                    add(names, products.matchInvoiceDescription(pattern, brand, page), query.brand(), "Similar description - confirm product");
                }
            }
        }
        return new Match(names.isEmpty() ? "NOT_FOUND" : "CHOOSE_PRODUCT", new ArrayList<>(names.values()));
    }

    private void add(LinkedHashMap<Long, Candidate> result, List<ProductEntity> found, String brand, String reason) {
        found.stream().filter(p -> brand == null || brand.isBlank() || p.getBrand().getName().strip().equalsIgnoreCase(brand.strip()))
            .forEach(p -> result.putIfAbsent(p.getId(), new Candidate(p.getId(), label(p), reason)));
    }

    static String normalizePart(String value) {
        return value == null ? "" : value.replaceAll("[\\s\\-_/\\.]", "").toUpperCase(Locale.ROOT);
    }

    static String descriptionPattern(String value) {
        String normalized = java.text.Normalizer.normalize(value, java.text.Normalizer.Form.NFKC)
            .toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]+", " ").strip();
        if (normalized.length() < 3) return null;
        String[] words = normalized.split("\\s+");
        if (words.length > 12) return null;
        return "%" + String.join("%", words) + "%";
    }

    private String label(ProductEntity p) {
        return p.getName() + " | Brand: " + p.getBrand().getName() + " | Part: " + p.getPartNumber()
            + (p.getPosition() == null ? "" : " | " + p.getPosition().getDisplayName());
    }
}
