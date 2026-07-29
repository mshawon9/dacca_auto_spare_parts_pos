package com.daccaauto.pos.service.serviceImpl;

import com.daccaauto.pos.dto.product.ProductCreateRequest;
import com.daccaauto.pos.dto.product.ProductDetailsResponse;
import com.daccaauto.pos.dto.product.ProductResponse;
import com.daccaauto.pos.dto.product.ProductUpdateRequest;
import com.daccaauto.pos.entity.*;
import com.daccaauto.pos.exception.DuplicateResourceException;
import com.daccaauto.pos.exception.ResourceNotFoundException;
import com.daccaauto.pos.repository.*;
import com.daccaauto.pos.service.ProductService;
import com.daccaauto.pos.service.ProductImageStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductServiceImpl implements ProductService {

    private static final Pattern YEAR_PATTERN = Pattern.compile("\\b(19\\d{2}|20\\d{2}|2100)\\b");

    private final ProductApplicationRepository productApplicationRepository;
    private final ProductCategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
    private final BrandCategoryRepository brandCategoryRepository;
    private final VehicleApplicationRepository vehicleApplicationRepository;
    private final ProductRepository productRepository;
    private final ProductSimilarityRepository productSimilarityRepository;
    private final ProductGroupRepository productGroupRepository;
    private final ProductImageStorageService productImageStorageService;
    private final ProductStockRepository productStockRepository;
    private final StockAdjustmentRepository stockAdjustmentRepository;
    private final ProductPriceHistoryRepository productPriceHistoryRepository;
    private final ProductAlternativePartNumberRepository productAlternativePartNumberRepository;

    @Override
    public ProductResponse create(ProductCreateRequest request) {
        ProductCategoryEntity category = categoryRepository.findById(request.getCategoryId())
            .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + request.getCategoryId()));

        BrandEntity brand = brandRepository.findById(request.getBrandId())
            .orElseThrow(() -> new ResourceNotFoundException("Brand not found: " + request.getBrandId()));

        validateBrandAllowedForCategory(request.getBrandId(), request.getCategoryId());

        String partNumber = removePartNumberSpaces(request.getPartNumber());
        String normalizedPartNumber = normalizePartNumber(partNumber);

        if (productRepository.existsByBrandIdAndNormalizedPartNumber(request.getBrandId(), normalizedPartNumber)) {
            throw new DuplicateResourceException("Same brand cannot have duplicate part number");
        }

        ProductEntity entity = new ProductEntity();
        entity.setName(buildCategoryProductName(category, request.getName()));
        entity.setSpecLabel(trimToNull(request.getSpecLabel()));
        entity.setPosition(trimToNull(request.getPosition()));
        entity.setDimension(trimToNull(request.getDimension()));
        entity.setSku(trimToNull(request.getSku()));
        entity.setReorderLevel(defaultReorderLevel(request.getReorderLevel()));
        entity.setPartNumber(partNumber);
        entity.setAlternativePartNumber(trimToNull(request.getAlternativePartNumber()));
        entity.setBarcode(resolveBarcode(category, request.getBarcode(), null));
        entity.setDescription(trimToNull(request.getDescription()));
        entity.setCategory(category);
        entity.setBrand(brand);
        entity.setProductGroup(resolveProductGroup(category, entity.getName(), request.getPosition(), request.getDimension(), request.getSimilarProductId()));
        entity.setActive(request.getActive() == null || request.getActive());

        ProductEntity saved = productRepository.save(entity);
        storeImage(saved, request.getImage());

        syncApplications(saved, request.getApplicationIds());
        syncAlternativePartNumbers(saved, request.getAlternativePartNumber());

        return map(saved);
    }

    @Override
    public ProductResponse update(Long id, ProductUpdateRequest request) {
        ProductEntity entity = productRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));

        ProductCategoryEntity category = categoryRepository.findById(request.getCategoryId())
            .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + request.getCategoryId()));

        BrandEntity brand = brandRepository.findById(request.getBrandId())
            .orElseThrow(() -> new ResourceNotFoundException("Brand not found: " + request.getBrandId()));

        validateBrandAllowedForCategory(request.getBrandId(), request.getCategoryId());

        String partNumber = removePartNumberSpaces(request.getPartNumber());
        String normalizedPartNumber = normalizePartNumber(partNumber);

        if (productRepository.existsByBrandIdAndNormalizedPartNumberAndIdNot(
            request.getBrandId(), normalizedPartNumber, id)) {
            throw new DuplicateResourceException("Same brand cannot have duplicate part number");
        }

        entity.setName(buildCategoryProductName(category, request.getName()));
        entity.setSpecLabel(trimToNull(request.getSpecLabel()));
        entity.setPosition(trimToNull(request.getPosition()));
        entity.setDimension(trimToNull(request.getDimension()));
        entity.setSku(trimToNull(request.getSku()));
        entity.setReorderLevel(defaultReorderLevel(request.getReorderLevel()));
        entity.setPartNumber(partNumber);
        entity.setAlternativePartNumber(trimToNull(request.getAlternativePartNumber()));
        entity.setBarcode(resolveBarcode(category, request.getBarcode(), id));
        entity.setDescription(trimToNull(request.getDescription()));
        entity.setCategory(category);
        entity.setBrand(brand);
        entity.setProductGroup(resolveProductGroup(category, entity.getName(), request.getPosition(), request.getDimension(), request.getSimilarProductId()));
        entity.setActive(request.getActive() == null || request.getActive());

        ProductEntity saved = productRepository.save(entity);
        updateImage(saved, request);

        syncApplications(saved, request.getApplicationIds());
        syncAlternativePartNumbers(saved, request.getAlternativePartNumber());

        return map(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getById(Long id) {
        ProductEntity entity = productRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));

        return map(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductDetailsResponse getDetails(Long id) {
        ProductResponse product = getById(id);
        List<ProductDetailsResponse.PartNumberGroup> partNumberGroups = buildPartNumberGroups(product);
        int partNumberCount = partNumberGroups.stream()
            .mapToInt(group -> 1 + group.alternativePartNumbers().size())
            .sum();
        List<ProductDetailsResponse.ProductVariantSummary> variants = buildProductVariants(product);
        BigDecimal variantTotalStockQuantity = variants.stream()
            .map(ProductDetailsResponse.ProductVariantSummary::totalStockQuantity)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<ProductDetailsResponse.StockSummary> stockSummaries = productStockRepository
            .findByProductIdOrderByStoreNameAsc(id)
            .stream()
            .map(stock -> new ProductDetailsResponse.StockSummary(
                stock.getStore().getId(),
                stock.getStore().getName(),
                stock.getQuantity(),
                stock.getSellingPrice()
            ))
            .toList();

        BigDecimal totalStockQuantity = stockSummaries.stream()
            .map(ProductDetailsResponse.StockSummary::quantity)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<ProductDetailsResponse.PriceHistorySummary> priceHistories = productPriceHistoryRepository
            .findTop3ByProductIdOrderByCreatedAtDesc(id)
            .stream()
            .map(history -> new ProductDetailsResponse.PriceHistorySummary(
                history.getStore().getName(),
                history.getOldPrice(),
                history.getNewPrice(),
                history.getNote(),
                history.getCreatedAt()
            ))
            .toList();

        return new ProductDetailsResponse(
            product,
            partNumberGroups,
            partNumberCount,
            variants,
            variantTotalStockQuantity,
            totalStockQuantity,
            stockSummaries,
            priceHistories
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse.SimilarProductSummary> getSimilarityGroup(Long productId) {
        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("Product not found: " + productId);
        }

        return mapSimilarityGroup(productId);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductImage getImage(Long productId) {
        ProductEntity product = productRepository.findById(productId)
            .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + productId));

        if (product.getImageFileName() == null) {
            throw new ResourceNotFoundException("Product image not found: " + productId);
        }

        return new ProductImage(
            productImageStorageService.load(product.getImageFileName()),
            product.getImageContentType()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ProductResponse> getLastCreatedProduct() {
        return productRepository.findTopByOrderByCreatedAtDescIdDesc()
            .map(this::map);
    }

    @Override
    @Transactional(readOnly = true)
    public String suggestBarcode(Long categoryId) {
        ProductCategoryEntity category = categoryRepository.findById(categoryId)
            .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + categoryId));
        return generateBarcode(category);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> search(String keyword,
                                        Long categoryId,
                                        Long brandId,
                                        Long applicationId,
                                        Boolean active) {

        String keywordPattern = normalizeKeywordPattern(keyword);
        ApplicationKeyword applicationKeyword = parseApplicationKeyword(keyword);

        List<ProductEntity> products = productRepository.search(
                keywordPattern,
                applicationKeyword.pattern(),
                applicationKeyword.year(),
                categoryId,
                brandId,
                applicationId,
                active
        );

        if (products == null || products.isEmpty()) {
            return List.of();
        }

        return products.stream()
                .map(this::map)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> searchPage(String keyword,
                                            Long categoryId,
                                            Long brandId,
                                            Long applicationId,
                                            Boolean active,
                                            Pageable pageable) {
        ApplicationKeyword applicationKeyword = parseApplicationKeyword(keyword);

        return productRepository.searchPage(
                normalizeKeywordPattern(keyword),
                applicationKeyword.pattern(),
                applicationKeyword.year(),
                categoryId,
                brandId,
                applicationId,
                active,
                pageable
        ).map(this::map);
    }

    private String normalizeKeywordPattern(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        return "%" + keyword.trim().toLowerCase(java.util.Locale.ROOT) + "%";
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        return keyword.trim();
    }

    private ApplicationKeyword parseApplicationKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return new ApplicationKeyword(null, null);
        }

        Matcher matcher = YEAR_PATTERN.matcher(keyword);
        if (!matcher.find()) {
            return new ApplicationKeyword(null, null);
        }

        Integer year = Integer.valueOf(matcher.group(1));
        String text = matcher.replaceAll(" ").trim().replaceAll("\\s+", " ");
        if (text.isBlank()) {
            return new ApplicationKeyword(null, year);
        }

        return new ApplicationKeyword("%" + text.toLowerCase(Locale.ROOT) + "%", year);
    }

    @Override
    public void delete(Long id) {
        ProductEntity entity = productRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));

        if (productStockRepository.existsByProductId(id)
            || stockAdjustmentRepository.existsByProductId(id)
            || productPriceHistoryRepository.existsByProductId(id)) {
            throw new DuplicateResourceException(
                "Product has inventory history and cannot be deleted. Mark it inactive instead."
            );
        }

        productApplicationRepository.deleteByProductId(id);
        productAlternativePartNumberRepository.deleteByProductId(id);
        productSimilarityRepository.deleteAllForProduct(id);
        productRepository.delete(entity);
        productImageStorageService.delete(entity.getImageFileName());
    }

    private void validateBrandAllowedForCategory(Long brandId, Long categoryId) {
        if (!brandCategoryRepository.existsByBrandIdAndCategoryIdAndActiveTrue(brandId, categoryId)) {
            throw new DuplicateResourceException("Selected brand is not allowed for the selected category");
        }
    }

    private void syncApplications(ProductEntity product, Set<Long> applicationIds) {
        productApplicationRepository.deleteByProductId(product.getId());
        productApplicationRepository.flush();

        if (applicationIds == null || applicationIds.isEmpty()) {
            return;
        }

        Set<Long> uniqueIds = new LinkedHashSet<>(applicationIds);

        for (Long applicationId : uniqueIds) {
            VehicleApplicationEntity vehicleApplication = vehicleApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle application not found: " + applicationId));

            ProductApplicationEntity join = new ProductApplicationEntity();
            join.setProduct(product);
            join.setVehicleApplication(vehicleApplication);

            productApplicationRepository.save(join);
        }
    }

    private void syncAlternativePartNumbers(ProductEntity product, String alternativePartNumbers) {
        productAlternativePartNumberRepository.deleteByProductId(product.getId());
        productAlternativePartNumberRepository.flush();

        for (String partNumber : parseAlternativePartNumbers(alternativePartNumbers)) {
            ProductAlternativePartNumberEntity alternative = new ProductAlternativePartNumberEntity();
            alternative.setProduct(product);
            alternative.setPartNumber(partNumber);
            productAlternativePartNumberRepository.save(alternative);
        }
    }

    private void syncSimilarProduct(ProductEntity product, Long similarProductId) {
        productSimilarityRepository.deleteByProductOneId(product.getId());

        if (similarProductId == null) {
            return;
        }

        if (product.getId().equals(similarProductId)) {
            throw new DuplicateResourceException("A product cannot be similar to itself");
        }

        ProductEntity similarProduct = productRepository.findById(similarProductId)
            .orElseThrow(() -> new ResourceNotFoundException("Similar product not found: " + similarProductId));

        ProductSimilarityEntity similarity = new ProductSimilarityEntity();
        similarity.setProductOne(product);
        similarity.setProductTwo(similarProduct);
        productSimilarityRepository.save(similarity);
    }

    private ProductResponse map(ProductEntity entity) {
        List<ProductApplicationEntity> applications = productApplicationRepository.findByProductIdWithApplication(entity.getId());

        Set<Long> applicationIds = applications.stream()
            .map(pa -> pa.getVehicleApplication().getId())
            .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));

        List<String> applicationNames = applications.stream()
            .map(pa -> buildApplicationDisplayName(pa.getVehicleApplication()))
            .toList();

        Long selectedSimilarProductId = productSimilarityRepository.findSelectedSimilarity(entity.getId())
            .map(similarity -> similarity.getProductTwo().getId())
            .orElse(null);

        List<ProductResponse.SimilarProductSummary> similarProducts = mapSimilarityGroup(entity.getId());
        List<String> alternativePartNumbers = getAlternativePartNumbers(entity);

        return new ProductResponse(
            entity.getId(),
            entity.getName(),
            entity.getSpecLabel(),
            entity.getPosition(),
            entity.getDimension(),
            entity.getSku(),
            defaultReorderLevel(entity.getReorderLevel()),
            entity.getPartNumber(),
            buildAlternativePartNumberSummary(alternativePartNumbers),
            alternativePartNumbers,
            entity.getBarcode(),
            entity.getDescription(),
            entity.getImageFileName() != null,
            entity.getCategory().getId(),
            entity.getCategory().getName(),
            entity.getBrand().getId(),
            entity.getBrand().getName(),
            entity.getProductGroup() == null ? null : entity.getProductGroup().getId(),
            entity.getProductGroup() == null ? null : entity.getProductGroup().getName(),
            applicationIds,
            applicationNames,
            buildApplicationSummary(applicationNames),
            selectedSimilarProductId,
            similarProducts,
            entity.isActive()
        );
    }

    private List<ProductResponse.SimilarProductSummary> mapSimilarityGroup(Long productId) {
        return productRepository
            .findAllById(productSimilarityRepository.findSimilarityGroupProductIds(productId))
            .stream()
            .sorted(java.util.Comparator.comparing(ProductEntity::getName, String.CASE_INSENSITIVE_ORDER))
            .map(product -> new ProductResponse.SimilarProductSummary(
                product.getId(),
                buildProductDisplayName(product)
            ))
            .toList();
    }

    private List<ProductDetailsResponse.PartNumberGroup> buildPartNumberGroups(ProductResponse product) {
        List<ProductEntity> variants = product.productGroupId() == null
            ? productRepository.findAllById(List.of(product.id()))
            : productRepository.findByProductGroupIdOrderByBrandNameAscPartNumberAsc(product.productGroupId());

        return variants.stream()
            .map(variant -> new ProductDetailsResponse.PartNumberGroup(
                variant.getId(),
                buildProductDisplayName(variant),
                variant.getPartNumber(),
                getAlternativePartNumbers(variant),
                variant.getId().equals(product.id())
            ))
            .toList();
    }

    private List<ProductDetailsResponse.ProductVariantSummary> buildProductVariants(ProductResponse product) {
        if (product.productGroupId() == null) {
            return List.of(new ProductDetailsResponse.ProductVariantSummary(
                product.id(),
                product.name(),
                product.brandName(),
                product.partNumber(),
                product.alternativePartNumbers(),
                product.position(),
                product.dimension(),
                productStockRepository.sumQuantityByProductId(product.id()),
                true
            ));
        }

        return productRepository.findByProductGroupIdOrderByBrandNameAscPartNumberAsc(product.productGroupId())
            .stream()
            .map(variant -> new ProductDetailsResponse.ProductVariantSummary(
                variant.getId(),
                variant.getName(),
                variant.getBrand().getName(),
                variant.getPartNumber(),
                getAlternativePartNumbers(variant),
                variant.getPosition(),
                variant.getDimension(),
                productStockRepository.sumQuantityByProductId(variant.getId()),
                variant.getId().equals(product.id())
            ))
            .toList();
    }

    private ProductGroupEntity resolveProductGroup(ProductCategoryEntity category,
                                                   String productName,
                                                   String position,
                                                   String dimension,
                                                   Long similarProductId) {
        if (similarProductId != null) {
            ProductEntity similarProduct = productRepository.findById(similarProductId)
                .orElseThrow(() -> new ResourceNotFoundException("Similar product not found: " + similarProductId));
            if (similarProduct.getProductGroup() != null) {
                return similarProduct.getProductGroup();
            }

            ProductGroupEntity group = findOrCreateProductGroup(
                similarProduct.getCategory(),
                similarProduct.getName(),
                similarProduct.getPosition(),
                similarProduct.getDimension()
            );
            similarProduct.setProductGroup(group);
            productRepository.save(similarProduct);
            return group;
        }

        return findOrCreateProductGroup(category, productName, position, dimension);
    }

    private ProductGroupEntity findOrCreateProductGroup(ProductCategoryEntity category,
                                                        String productName,
                                                        String position,
                                                        String dimension) {
        String normalizedKey = buildProductGroupKey(productName, position, dimension);
        return productGroupRepository.findByCategoryIdAndNormalizedKey(category.getId(), normalizedKey)
            .orElseGet(() -> {
                ProductGroupEntity group = new ProductGroupEntity();
                group.setCategory(category);
                group.setName(productName);
                group.setNormalizedKey(normalizedKey);
                return productGroupRepository.save(group);
            });
    }

    private String buildProductGroupKey(String productName, String position, String dimension) {
        return java.util.stream.Stream.of(productName, position, dimension)
            .filter(value -> value != null && !value.isBlank())
            .map(value -> value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", ""))
            .collect(java.util.stream.Collectors.joining("|"));
    }

    private String buildProductDisplayName(ProductEntity product) {
        return java.util.stream.Stream.of(
                product.getCategory().getName(),
                product.getName(),
                product.getPartNumber(),
                product.getPosition(),
                product.getDimension()
            )
            .filter(value -> value != null && !value.isBlank())
            .collect(java.util.stream.Collectors.joining(" | "));
    }

    private void storeImage(ProductEntity product, org.springframework.web.multipart.MultipartFile image) {
        if (image == null || image.isEmpty()) {
            return;
        }

        ProductImageStorageService.StoredImage storedImage = productImageStorageService.store(image);
        product.setImageFileName(storedImage.fileName());
        product.setImageContentType(storedImage.contentType());
        productRepository.save(product);
    }

    private void updateImage(ProductEntity product, ProductUpdateRequest request) {
        String oldFileName = product.getImageFileName();

        if (Boolean.TRUE.equals(request.getRemoveImage())) {
            product.setImageFileName(null);
            product.setImageContentType(null);
            productRepository.save(product);
            productImageStorageService.delete(oldFileName);
            oldFileName = null;
        }

        if (request.getImage() != null && !request.getImage().isEmpty()) {
            ProductImageStorageService.StoredImage storedImage = productImageStorageService.store(request.getImage());
            product.setImageFileName(storedImage.fileName());
            product.setImageContentType(storedImage.contentType());
            productRepository.save(product);
            productImageStorageService.delete(oldFileName);
        }
    }

    private String buildApplicationSummary(List<String> applications) {
        if (applications == null || applications.isEmpty()) {
            return "-";
        }
        if (applications.size() == 1) {
            return applications.get(0);
        }
        return applications.get(0) + " + " + (applications.size() - 1) + " more";
    }

    private String buildApplicationDisplayName(VehicleApplicationEntity application) {
        String makeName = application.getVehicleMake() != null ? application.getVehicleMake().getName() : null;

        return java.util.stream.Stream.of(makeName, application.getDisplayName())
            .filter(value -> value != null && !value.isBlank())
            .collect(java.util.stream.Collectors.joining(" "));
    }

    private String normalizePartNumber(String input) {
        return input.replaceAll("[\\s\\-_/\\.]", "").toUpperCase(Locale.ROOT);
    }

    private BigDecimal defaultReorderLevel(BigDecimal reorderLevel) {
        return reorderLevel == null ? BigDecimal.valueOf(2) : reorderLevel;
    }

    private String removePartNumberSpaces(String input) {
        return input == null ? null : input.replaceAll("\\s+", "");
    }

    private List<String> getAlternativePartNumbers(ProductEntity product) {
        List<String> values = productAlternativePartNumberRepository.findByProductIdOrderByPartNumberAsc(product.getId())
            .stream()
            .map(ProductAlternativePartNumberEntity::getPartNumber)
            .toList();

        if (!values.isEmpty()) {
            return values;
        }

        return parseAlternativePartNumbers(product.getAlternativePartNumber()).stream().toList();
    }

    private Set<String> parseAlternativePartNumbers(String input) {
        if (input == null || input.isBlank()) {
            return java.util.Set.of();
        }

        return java.util.Arrays.stream(input.split(","))
            .map(String::trim)
            .filter(value -> !value.isBlank())
            .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    }

    private String buildAlternativePartNumberSummary(List<String> alternativePartNumbers) {
        if (alternativePartNumbers == null || alternativePartNumbers.isEmpty()) {
            return null;
        }
        return String.join(", ", alternativePartNumbers);
    }

    private String buildCategoryProductName(ProductCategoryEntity category, String productName) {
        String name = productName == null ? "" : productName.trim();
        String categoryName = category.getName().trim();

        if (name.toLowerCase(Locale.ROOT).startsWith(categoryName.toLowerCase(Locale.ROOT) + " ")) {
            return name;
        }

        return (categoryName + " " + name).trim();
    }

    private String resolveBarcode(ProductCategoryEntity category, String requestedBarcode, Long productId) {
        String barcode = trimToNull(requestedBarcode);
        if (barcode == null) {
            return generateBarcode(category);
        }

        boolean duplicate = productId == null
            ? productRepository.existsByBarcode(barcode)
            : productRepository.existsByBarcodeAndIdNot(barcode, productId);

        if (duplicate) {
            throw new DuplicateResourceException("Barcode already exists: " + barcode);
        }

        return barcode;
    }

    private String generateBarcode(ProductCategoryEntity category) {
        String prefix = buildCategoryBarcodePrefix(category);
        long next = productRepository.countByCategoryId(category.getId()) + 1;
        String barcode;

        do {
            barcode = prefix + "-" + String.format("%06d", next++);
        } while (productRepository.existsByBarcode(barcode));

        return barcode;
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

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private record ApplicationKeyword(String pattern, Integer year) {
    }
}
