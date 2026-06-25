package com.daccaauto.pos.service.serviceImpl;

import com.daccaauto.pos.dto.product.ProductCreateRequest;
import com.daccaauto.pos.dto.product.ProductResponse;
import com.daccaauto.pos.dto.product.ProductUpdateRequest;
import com.daccaauto.pos.entity.*;
import com.daccaauto.pos.exception.DuplicateResourceException;
import com.daccaauto.pos.exception.ResourceNotFoundException;
import com.daccaauto.pos.repository.*;
import com.daccaauto.pos.service.ProductService;
import com.daccaauto.pos.service.ProductImageStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductServiceImpl implements ProductService {

    private final ProductApplicationRepository productApplicationRepository;
    private final ProductCategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
    private final BrandCategoryRepository brandCategoryRepository;
    private final VehicleApplicationRepository vehicleApplicationRepository;
    private final ProductRepository productRepository;
    private final ProductSimilarityRepository productSimilarityRepository;
    private final ProductImageStorageService productImageStorageService;
    private final ProductStockRepository productStockRepository;
    private final StockAdjustmentRepository stockAdjustmentRepository;
    private final ProductPriceHistoryRepository productPriceHistoryRepository;

    @Override
    public ProductResponse create(ProductCreateRequest request) {
        ProductCategoryEntity category = categoryRepository.findById(request.getCategoryId())
            .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + request.getCategoryId()));

        BrandEntity brand = brandRepository.findById(request.getBrandId())
            .orElseThrow(() -> new ResourceNotFoundException("Brand not found: " + request.getBrandId()));

        validateBrandAllowedForCategory(request.getBrandId(), request.getCategoryId());

        String normalizedPartNumber = normalizePartNumber(request.getPartNumber());

        if (productRepository.existsByBrandIdAndNormalizedPartNumber(request.getBrandId(), normalizedPartNumber)) {
            throw new DuplicateResourceException("Same brand cannot have duplicate part number");
        }

        ProductEntity entity = new ProductEntity();
        entity.setName(request.getName().trim());
        entity.setSpecLabel(trimToNull(request.getSpecLabel()));
        entity.setDimension(trimToNull(request.getDimension()));
        entity.setSku(trimToNull(request.getSku()));
        entity.setPartNumber(request.getPartNumber().trim());
        entity.setBarcode(trimToNull(request.getBarcode()));
        entity.setDescription(trimToNull(request.getDescription()));
        entity.setCategory(category);
        entity.setBrand(brand);
        entity.setActive(request.getActive() == null || request.getActive());

        ProductEntity saved = productRepository.save(entity);
        storeImage(saved, request.getImage());

        syncApplications(saved, request.getApplicationIds());
        syncSimilarProduct(saved, request.getSimilarProductId());

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

        String normalizedPartNumber = normalizePartNumber(request.getPartNumber());

        if (productRepository.existsByBrandIdAndNormalizedPartNumberAndIdNot(
            request.getBrandId(), normalizedPartNumber, id)) {
            throw new DuplicateResourceException("Same brand cannot have duplicate part number");
        }

        entity.setName(request.getName().trim());
        entity.setSpecLabel(trimToNull(request.getSpecLabel()));
        entity.setDimension(trimToNull(request.getDimension()));
        entity.setSku(trimToNull(request.getSku()));
        entity.setPartNumber(request.getPartNumber().trim());
        entity.setBarcode(trimToNull(request.getBarcode()));
        entity.setDescription(trimToNull(request.getDescription()));
        entity.setCategory(category);
        entity.setBrand(brand);
        entity.setActive(request.getActive() == null || request.getActive());

        ProductEntity saved = productRepository.save(entity);
        updateImage(saved, request);

        syncApplications(saved, request.getApplicationIds());
        syncSimilarProduct(saved, request.getSimilarProductId());

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
    public List<ProductResponse> search(String keyword,
                                        Long categoryId,
                                        Long brandId,
                                        Long applicationId,
                                        Boolean active) {

        String keywordPattern = normalizeKeywordPattern(keyword);

        List<ProductEntity> products = productRepository.search(
                keywordPattern,
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
            .map(pa -> pa.getVehicleApplication().getDisplayName())
            .toList();

        Long selectedSimilarProductId = productSimilarityRepository.findSelectedSimilarity(entity.getId())
            .map(similarity -> similarity.getProductTwo().getId())
            .orElse(null);

        List<ProductResponse.SimilarProductSummary> similarProducts = mapSimilarityGroup(entity.getId());

        return new ProductResponse(
            entity.getId(),
            entity.getName(),
            entity.getSpecLabel(),
            entity.getDimension(),
            entity.getSku(),
            entity.getPartNumber(),
            entity.getBarcode(),
            entity.getDescription(),
            entity.getImageFileName() != null,
            entity.getCategory().getId(),
            entity.getCategory().getName(),
            entity.getBrand().getId(),
            entity.getBrand().getName(),
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

    private String buildProductDisplayName(ProductEntity product) {
        return java.util.stream.Stream.of(
                product.getName(),
                product.getPartNumber(),
                product.getSpecLabel(),
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

    private String normalizePartNumber(String input) {
        return input.replaceAll("[\\s\\-_/\\.]", "").toUpperCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
