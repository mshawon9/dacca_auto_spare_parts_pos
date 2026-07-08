package com.daccaauto.pos.service.serviceImpl;

import com.daccaauto.pos.dto.product.ProductImportResult;
import com.daccaauto.pos.entity.BrandCategoryEntity;
import com.daccaauto.pos.entity.BrandEntity;
import com.daccaauto.pos.entity.ProductAlternativePartNumberEntity;
import com.daccaauto.pos.entity.ProductCategoryEntity;
import com.daccaauto.pos.entity.ProductEntity;
import com.daccaauto.pos.entity.ProductStockEntity;
import com.daccaauto.pos.entity.StoreEntity;
import com.daccaauto.pos.repository.BrandCategoryRepository;
import com.daccaauto.pos.repository.BrandRepository;
import com.daccaauto.pos.repository.ProductAlternativePartNumberRepository;
import com.daccaauto.pos.repository.ProductCategoryRepository;
import com.daccaauto.pos.repository.ProductRepository;
import com.daccaauto.pos.repository.ProductStockRepository;
import com.daccaauto.pos.repository.StoreRepository;
import com.daccaauto.pos.service.ProductImportService;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductImportServiceImpl implements ProductImportService {

    private static final int MAX_ROWS = 500;
    private static final String[] SAMPLE_HEADERS = {
        "Category",
        "Brand",
        "Product Name",
        "Part Number",
        "Position",
        "Dimension",
        "SKU",
        "Reorder Level",
        "Warehouse",
        "Current Stock",
        "Cost Price",
        "Barcode",
        "Alternative Part Number",
        "Description",
        "Active"
    };

    private final ProductRepository productRepository;
    private final ProductCategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
    private final BrandCategoryRepository brandCategoryRepository;
    private final ProductAlternativePartNumberRepository alternativePartNumberRepository;
    private final ProductStockRepository productStockRepository;
    private final StoreRepository storeRepository;
    private final CacheManager cacheManager;

    @Override
    @Transactional(readOnly = true)
    public byte[] buildSampleTemplate() {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Products");
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            Row header = sheet.createRow(0);
            for (int i = 0; i < SAMPLE_HEADERS.length; i++) {
                header.createCell(i).setCellValue(SAMPLE_HEADERS[i]);
                header.getCell(i).setCellStyle(headerStyle);
            }

            addSampleRow(sheet, 1, "Brake Pad", "Brembo", "Civic Front Pad", "BP1001", "Front", "38 X 25 X 9", "SKU-001", "2", "Main Store", "10", "35.50", "", "BP-ALT1, BP-ALT2", "Ceramic brake pad", "TRUE");
            addSampleRow(sheet, 2, "Shock Absorber", "KYB", "Corolla Rear Shock", "SA2001", "Rear", "", "SKU-002", "3", "Main Store", "5", "62.00", "", "", "Rear shock absorber", "TRUE");
            addSampleRow(sheet, 3, "Bearing", "NSK", "Yaris Wheel Bearing", "WB3001", "Front", "", "", "2", "", "", "", "", "WB-ALT1", "", "TRUE");

            for (int i = 0; i < SAMPLE_HEADERS.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("Could not build product import sample", ex);
        }
    }

    @Override
    public ProductImportResult importProducts(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return failWholeFile("Please choose an Excel file.");
        }

        if (!isExcelFile(file.getOriginalFilename())) {
            return failWholeFile("Only .xlsx files are supported.");
        }

        List<ImportRow> rows;
        try {
            rows = readRows(file);
        } catch (IOException ex) {
            return failWholeFile("Could not read Excel file.");
        }

        if (rows.isEmpty()) {
            return failWholeFile("No product rows found.");
        }

        if (rows.size() > MAX_ROWS) {
            return failWholeFile("Maximum " + MAX_ROWS + " products can be uploaded at once.");
        }

        validateRows(rows);
        if (rows.stream().anyMatch(row -> !row.errors.isEmpty())) {
            return toResult(rows);
        }

        Map<String, ProductCategoryEntity> categories = resolveCategories(rows);
        Map<String, BrandEntity> brands = resolveBrands(rows);
        Map<String, StoreEntity> stores = resolveStores(rows);
        validateDuplicates(rows, brands);

        Set<String> reservedBarcodes = new HashSet<>();
        Map<Long, Long> nextBarcodeNumbersByCategory = new HashMap<>();

        for (ImportRow row : rows) {
            if (!row.errors.isEmpty()) {
                continue;
            }

            ProductCategoryEntity category = categories.get(key(row.category));
            BrandEntity brand = brands.get(key(row.brand));
            ensureBrandCategoryMapping(category, brand);

            ProductEntity product = new ProductEntity();
            product.setName(buildCategoryProductName(category, row.name));
            product.setPosition(trimToNull(row.position));
            product.setDimension(trimToNull(row.dimension));
            product.setSku(trimToNull(row.sku));
            product.setReorderLevel(row.reorderLevel);
            product.setPartNumber(removeSpaces(row.partNumber));
            product.setAlternativePartNumber(trimToNull(row.alternativePartNumber));
            product.setBarcode(resolveBarcode(category, row.barcode, reservedBarcodes, nextBarcodeNumbersByCategory));
            product.setDescription(trimToNull(row.description));
            product.setCategory(category);
            product.setBrand(brand);
            product.setActive(row.active);

            ProductEntity saved = productRepository.save(product);
            syncAlternativePartNumbers(saved, row.alternativePartNumber);
            syncInitialStock(saved, stores.get(key(row.warehouse)), row);
            row.success = true;
            row.message = "Inserted successfully";
        }

        evictLookupCaches();
        return toResult(rows);
    }

    private void addSampleRow(Sheet sheet,
                              int rowIndex,
                              String category,
                              String brand,
                              String productName,
                              String partNumber,
                              String position,
                              String dimension,
                              String sku,
                              String reorderLevel,
                              String warehouse,
                              String currentStock,
                              String costPrice,
                              String barcode,
                              String alternativePartNumber,
                              String description,
                              String active) {
        Row row = sheet.createRow(rowIndex);
        String[] values = {
            category,
            brand,
            productName,
            partNumber,
            position,
            dimension,
            sku,
            reorderLevel,
            warehouse,
            currentStock,
            costPrice,
            barcode,
            alternativePartNumber,
            description,
            active
        };
        for (int i = 0; i < values.length; i++) {
            row.createCell(i).setCellValue(values[i]);
        }
    }

    private List<ImportRow> readRows(MultipartFile file) throws IOException {
        List<ImportRow> rows = new ArrayList<>();
        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            DataFormatter formatter = new DataFormatter();
            Map<String, Integer> headers = readHeaders(sheet.getRow(0), formatter, evaluator);

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row sheetRow = sheet.getRow(i);
                if (sheetRow == null || isBlankRow(sheetRow, formatter, evaluator)) {
                    continue;
                }
                rows.add(new ImportRow(
                    i + 1,
                    readCell(sheetRow, headers, "category", formatter, evaluator),
                    readCell(sheetRow, headers, "brand", formatter, evaluator),
                    readCell(sheetRow, headers, "name", formatter, evaluator),
                    readCell(sheetRow, headers, "partnumber", formatter, evaluator),
                    readCell(sheetRow, headers, "position", formatter, evaluator),
                    readCell(sheetRow, headers, "dimension", formatter, evaluator),
                    readCell(sheetRow, headers, "sku", formatter, evaluator),
                    readCell(sheetRow, headers, "reorderlevel", formatter, evaluator),
                    readCell(sheetRow, headers, "warehouse", formatter, evaluator),
                    readCell(sheetRow, headers, "currentstock", formatter, evaluator),
                    readCell(sheetRow, headers, "costprice", formatter, evaluator),
                    readCell(sheetRow, headers, "barcode", formatter, evaluator),
                    readCell(sheetRow, headers, "alternativepartnumber", formatter, evaluator),
                    readCell(sheetRow, headers, "description", formatter, evaluator),
                    parseActive(readCell(sheetRow, headers, "active", formatter, evaluator))
                ));
            }
        }
        return rows;
    }

    private Map<String, Integer> readHeaders(Row headerRow, DataFormatter formatter, FormulaEvaluator evaluator) {
        Map<String, Integer> headers = new HashMap<>();
        if (headerRow == null) {
            return headers;
        }
        for (int i = 0; i < headerRow.getLastCellNum(); i++) {
            String header = normalizeHeader(formatter.formatCellValue(headerRow.getCell(i), evaluator));
            if (!header.isBlank()) {
                headers.put(header, i);
            }
        }
        return headers;
    }

    private boolean isBlankRow(Row row, DataFormatter formatter, FormulaEvaluator evaluator) {
        for (int i = 0; i < row.getLastCellNum(); i++) {
            if (!formatter.formatCellValue(row.getCell(i), evaluator).trim().isBlank()) {
                return false;
            }
        }
        return true;
    }

    private String readCell(Row row,
                            Map<String, Integer> headers,
                            String column,
                            DataFormatter formatter,
                            FormulaEvaluator evaluator) {
        Integer index = headers.get(column);
        if (index == null) {
            return "";
        }
        return formatter.formatCellValue(row.getCell(index), evaluator).trim();
    }

    private void validateRows(List<ImportRow> rows) {
        Set<String> duplicateKeys = new HashSet<>();
        for (ImportRow row : rows) {
            require(row, row.category, "Category is required");
            require(row, row.brand, "Brand is required");
            require(row, row.name, "Product Name is required");
            require(row, row.partNumber, "Part Number is required");

            row.partNumber = removeSpaces(row.partNumber);
            if (!row.partNumber.matches("^[A-Za-z0-9._/\\-]+$")) {
                row.errors.add("Part Number has unsupported characters");
            }
            if (!row.barcode.isBlank() && !row.barcode.matches("^[A-Za-z0-9\\-]+$")) {
                row.errors.add("Barcode must be alphanumeric or hyphen");
            }
            row.reorderLevel = parseReorderLevel(row.reorderLevelInput, row);
            row.currentStock = parseCurrentStock(row.currentStockInput, row);
            row.costPrice = parseCostPrice(row.costPriceInput, row);
            if (row.hasStockInfo() && row.warehouse.isBlank()) {
                row.errors.add("Warehouse is required when Current Stock or Cost Price is provided");
            }
            if (row.name.length() > 200) row.errors.add("Product Name must not exceed 200 characters");
            if (row.category.length() > 100) row.errors.add("Category must not exceed 100 characters");
            if (row.brand.length() > 100) row.errors.add("Brand must not exceed 100 characters");
            if (row.warehouse.length() > 120) row.errors.add("Warehouse must not exceed 120 characters");
            if (row.position.length() > 80) row.errors.add("Position must not exceed 80 characters");
            if (row.dimension.length() > 120) row.errors.add("Dimension must not exceed 120 characters");
            if (row.sku.length() > 100) row.errors.add("SKU must not exceed 100 characters");
            if (row.alternativePartNumber.length() > 255) row.errors.add("Alternative Part Number must not exceed 255 characters");
            if (row.description.length() > 2000) row.errors.add("Description must not exceed 2000 characters");

            String batchKey = key(row.brand) + "|" + normalizePartNumber(row.partNumber);
            if (!duplicateKeys.add(batchKey)) {
                row.errors.add("Duplicate Brand + Part Number inside this Excel file");
            }
        }
    }

    private void validateDuplicates(List<ImportRow> rows, Map<String, BrandEntity> brands) {
        Set<String> barcodes = new HashSet<>();
        for (ImportRow row : rows) {
            BrandEntity brand = brands.get(key(row.brand));
            if (brand != null && productRepository.existsByBrandIdAndNormalizedPartNumber(
                brand.getId(), normalizePartNumber(row.partNumber))) {
                row.errors.add("Same brand already has this part number");
            }

            if (!row.barcode.isBlank()) {
                if (!barcodes.add(key(row.barcode)) || productRepository.existsByBarcode(row.barcode)) {
                    row.errors.add("Barcode already exists");
                }
            }
        }
    }

    private Map<String, ProductCategoryEntity> resolveCategories(List<ImportRow> rows) {
        Map<String, ProductCategoryEntity> categories = new LinkedHashMap<>();
        for (String categoryName : rows.stream().map(row -> row.category).distinct().toList()) {
            ProductCategoryEntity category = categoryRepository.findByNameIgnoreCase(categoryName)
                .orElseGet(() -> {
                    ProductCategoryEntity created = new ProductCategoryEntity();
                    created.setName(categoryName.trim());
                    created.setActive(true);
                    return categoryRepository.save(created);
                });
            categories.put(key(categoryName), category);
        }
        return categories;
    }

    private Map<String, BrandEntity> resolveBrands(List<ImportRow> rows) {
        Map<String, BrandEntity> brands = new LinkedHashMap<>();
        for (String brandName : rows.stream().map(row -> row.brand).distinct().toList()) {
            BrandEntity brand = brandRepository.findByNameIgnoreCase(brandName)
                .orElseGet(() -> {
                    BrandEntity created = new BrandEntity();
                    created.setName(brandName.trim());
                    created.setActive(true);
                    return brandRepository.save(created);
                });
            brands.put(key(brandName), brand);
        }
        return brands;
    }

    private Map<String, StoreEntity> resolveStores(List<ImportRow> rows) {
        Map<String, StoreEntity> stores = new LinkedHashMap<>();
        for (String storeName : rows.stream()
            .filter(ImportRow::hasStockInfo)
            .map(row -> row.warehouse)
            .filter(value -> value != null && !value.isBlank())
            .distinct()
            .toList()) {
            StoreEntity store = storeRepository.findByNameIgnoreCase(storeName)
                .orElseGet(() -> {
                    StoreEntity created = new StoreEntity();
                    created.setName(storeName.trim());
                    created.setActive(true);
                    return storeRepository.save(created);
                });
            stores.put(key(storeName), store);
        }
        return stores;
    }

    private void ensureBrandCategoryMapping(ProductCategoryEntity category, BrandEntity brand) {
        if (brandCategoryRepository.existsByBrandIdAndCategoryId(brand.getId(), category.getId())) {
            return;
        }

        BrandCategoryEntity mapping = new BrandCategoryEntity();
        mapping.setCategory(category);
        mapping.setBrand(brand);
        mapping.setActive(true);
        mapping.setDisplayOrder(1);
        brandCategoryRepository.save(mapping);
    }

    private String resolveBarcode(ProductCategoryEntity category,
                                  String requestedBarcode,
                                  Set<String> reservedBarcodes,
                                  Map<Long, Long> nextBarcodeNumbersByCategory) {
        String barcode = trimToNull(requestedBarcode);
        if (barcode != null) {
            reservedBarcodes.add(barcode);
            return barcode;
        }

        long next = nextBarcodeNumbersByCategory.computeIfAbsent(
            category.getId(), productRepository::countByCategoryId
        ) + 1;
        String generated;
        do {
            generated = buildCategoryBarcodePrefix(category) + "-" + String.format("%06d", next++);
        } while (reservedBarcodes.contains(generated) || productRepository.existsByBarcode(generated));

        nextBarcodeNumbersByCategory.put(category.getId(), next - 1);
        reservedBarcodes.add(generated);
        return generated;
    }

    private void syncAlternativePartNumbers(ProductEntity product, String alternativePartNumbers) {
        for (String partNumber : parseAlternativePartNumbers(alternativePartNumbers)) {
            ProductAlternativePartNumberEntity alternative = new ProductAlternativePartNumberEntity();
            alternative.setProduct(product);
            alternative.setPartNumber(partNumber);
            alternativePartNumberRepository.save(alternative);
        }
    }

    private void syncInitialStock(ProductEntity product, StoreEntity store, ImportRow row) {
        if (!row.hasStockInfo()) {
            return;
        }

        ProductStockEntity stock = new ProductStockEntity();
        stock.setProduct(product);
        stock.setStore(store);
        stock.setQuantity(row.currentStock == null ? BigDecimal.ZERO : row.currentStock);
        stock.setCostPrice(row.costPrice);
        productStockRepository.save(stock);
    }

    private Set<String> parseAlternativePartNumbers(String input) {
        if (input == null || input.isBlank()) {
            return Set.of();
        }
        return java.util.Arrays.stream(input.split(","))
            .map(String::trim)
            .filter(value -> !value.isBlank())
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private ProductImportResult toResult(List<ImportRow> rows) {
        List<ProductImportResult.RowResult> results = rows.stream()
            .map(row -> new ProductImportResult.RowResult(
                row.rowNumber,
                row.category,
                row.brand,
                row.name,
                row.partNumber,
                row.success,
                row.success ? row.message : String.join("; ", row.errors)
            ))
            .toList();

        int successCount = (int) rows.stream().filter(row -> row.success).count();
        return new ProductImportResult(rows.size(), successCount, rows.size() - successCount, results);
    }

    private ProductImportResult failWholeFile(String message) {
        return new ProductImportResult(
            0,
            0,
            1,
            List.of(new ProductImportResult.RowResult(0, "", "", "", "", false, message))
        );
    }

    private void require(ImportRow row, String value, String message) {
        if (value == null || value.isBlank()) {
            row.errors.add(message);
        }
    }

    private boolean isExcelFile(String filename) {
        return filename != null && filename.toLowerCase(Locale.ROOT).endsWith(".xlsx");
    }

    private String normalizeHeader(String value) {
        String normalized = value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
        return switch (normalized) {
            case "productname" -> "name";
            case "partno", "partnumber", "part" -> "partnumber";
            case "altpartnumber", "alternativepartno", "alternativepartnumbers", "altpartno" -> "alternativepartnumber";
            case "store", "storename", "warehouse", "warehousename" -> "warehouse";
            case "stock", "currentstock", "quantity", "currentquantity" -> "currentstock";
            case "cost", "costprice", "purchaseprice", "buyingprice" -> "costprice";
            default -> normalized;
        };
    }

    private Boolean parseActive(String value) {
        if (value == null || value.isBlank()) {
            return true;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return !List.of("false", "no", "n", "0", "inactive").contains(normalized);
    }

    private BigDecimal parseReorderLevel(String value, ImportRow row) {
        if (value == null || value.isBlank()) {
            return BigDecimal.valueOf(2);
        }
        try {
            BigDecimal parsed = new BigDecimal(value.trim());
            if (parsed.compareTo(BigDecimal.ZERO) < 0) {
                row.errors.add("Reorder Level cannot be negative");
                return BigDecimal.valueOf(2);
            }
            return parsed;
        } catch (NumberFormatException ex) {
            row.errors.add("Reorder Level must be a number");
            return BigDecimal.valueOf(2);
        }
    }

    private BigDecimal parseCurrentStock(String value, ImportRow row) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            BigDecimal parsed = new BigDecimal(value.trim());
            if (parsed.compareTo(BigDecimal.ZERO) < 0) {
                row.errors.add("Current Stock cannot be negative");
                return null;
            }
            if (parsed.stripTrailingZeros().scale() > 0) {
                row.errors.add("Current Stock must be a whole number");
                return null;
            }
            return parsed.setScale(0);
        } catch (NumberFormatException ex) {
            row.errors.add("Current Stock must be a number");
            return null;
        }
    }

    private BigDecimal parseCostPrice(String value, ImportRow row) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            BigDecimal parsed = new BigDecimal(value.trim());
            if (parsed.compareTo(BigDecimal.ZERO) < 0) {
                row.errors.add("Cost Price cannot be negative");
                return null;
            }
            return parsed.setScale(2, java.math.RoundingMode.UNNECESSARY);
        } catch (ArithmeticException | NumberFormatException ex) {
            row.errors.add("Cost Price must be a number with up to 2 decimal places");
            return null;
        }
    }

    private String buildCategoryProductName(ProductCategoryEntity category, String productName) {
        String name = productName == null ? "" : productName.trim();
        String categoryName = category.getName().trim();
        if (name.toLowerCase(Locale.ROOT).startsWith(categoryName.toLowerCase(Locale.ROOT) + " ")) {
            return name;
        }
        return (categoryName + " " + name).trim();
    }

    private String buildCategoryBarcodePrefix(ProductCategoryEntity category) {
        String code = category.getName() == null ? "" : category.getName()
            .replaceAll("[^A-Za-z0-9]", "")
            .toUpperCase(Locale.ROOT);
        if (code.length() >= 3) {
            return code.substring(0, 3);
        }
        return ("CAT" + category.getId()).substring(0, Math.min(6, ("CAT" + category.getId()).length()));
    }

    private String normalizePartNumber(String input) {
        return removeSpaces(input).replaceAll("[\\-_/\\.]", "").toUpperCase(Locale.ROOT);
    }

    private String removeSpaces(String input) {
        return input == null ? "" : input.replaceAll("\\s+", "");
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String key(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private void evictLookupCaches() {
        for (String cacheName : List.of("productCategories", "brandsByCategory")) {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
            }
        }
    }

    private static class ImportRow {
        private final int rowNumber;
        private final String category;
        private final String brand;
        private final String name;
        private String partNumber;
        private final String position;
        private final String dimension;
        private final String sku;
        private final String reorderLevelInput;
        private BigDecimal reorderLevel = BigDecimal.valueOf(2);
        private final String warehouse;
        private final String currentStockInput;
        private final String costPriceInput;
        private BigDecimal currentStock;
        private BigDecimal costPrice;
        private final String barcode;
        private final String alternativePartNumber;
        private final String description;
        private final boolean active;
        private boolean success;
        private String message = "";
        private final List<String> errors = new ArrayList<>();

        private ImportRow(int rowNumber,
                          String category,
                          String brand,
                          String name,
                          String partNumber,
                          String position,
                          String dimension,
                          String sku,
                          String reorderLevelInput,
                          String warehouse,
                          String currentStockInput,
                          String costPriceInput,
                          String barcode,
                          String alternativePartNumber,
                          String description,
                          boolean active) {
            this.rowNumber = rowNumber;
            this.category = category;
            this.brand = brand;
            this.name = name;
            this.partNumber = partNumber;
            this.position = position;
            this.dimension = dimension;
            this.sku = sku;
            this.reorderLevelInput = reorderLevelInput;
            this.warehouse = warehouse;
            this.currentStockInput = currentStockInput;
            this.costPriceInput = costPriceInput;
            this.barcode = barcode;
            this.alternativePartNumber = alternativePartNumber;
            this.description = description;
            this.active = active;
        }

        private boolean hasStockInfo() {
            return (currentStockInput != null && !currentStockInput.isBlank())
                || (costPriceInput != null && !costPriceInput.isBlank());
        }
    }
}
