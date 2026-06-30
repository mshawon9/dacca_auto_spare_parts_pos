package com.daccaauto.pos.controller;

import com.daccaauto.pos.dto.product.ProductCreateRequest;
import com.daccaauto.pos.dto.product.ProductDetailsResponse;
import com.daccaauto.pos.dto.product.ProductResponse;
import com.daccaauto.pos.dto.product.ProductUpdateRequest;
import com.daccaauto.pos.exception.DuplicateResourceException;
import com.daccaauto.pos.exception.ResourceNotFoundException;
import com.daccaauto.pos.exception.ProductImageException;
import com.daccaauto.pos.service.BrandCategoryService;
import com.daccaauto.pos.service.ProductCategoryService;
import com.daccaauto.pos.service.ProductService;
import com.daccaauto.pos.service.VehicleApplicationService;
import com.daccaauto.pos.service.VehicleMakeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Set;

@Controller
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final ProductCategoryService productCategoryService;
    private final BrandCategoryService brandCategoryService;
    private final VehicleApplicationService vehicleApplicationService;
    private final VehicleMakeService vehicleMakeService;

    @GetMapping
    public String list(@RequestParam(required = false) String keyword,
                       @RequestParam(required = false) Long categoryId,
                       @RequestParam(required = false) Long brandId,
                       @RequestParam(required = false) Long applicationId,
                       @RequestParam(required = false) Boolean active,
                       @RequestParam(defaultValue = "name") String sortBy,
                       @RequestParam(defaultValue = "asc") String sortDir,
                       @RequestParam(defaultValue = "0") int page,
                       Model model) {

        int pageSize = 15;
        int safePage = Math.max(page, 0);
        String selectedSortBy = normalizeProductSortBy(sortBy);
        String selectedSortDir = "desc".equalsIgnoreCase(sortDir) ? "desc" : "asc";

        Page<ProductResponse> productPage = productService.searchPage(
                keyword,
                categoryId,
                brandId,
                applicationId,
                active,
                PageRequest.of(safePage, pageSize, buildProductSort(selectedSortBy, selectedSortDir))
        );

        long totalItems = productPage.getTotalElements();
        long startItem = totalItems == 0 ? 0 : (long) productPage.getNumber() * productPage.getSize() + 1;
        long endItem = Math.min(startItem + productPage.getNumberOfElements() - 1, totalItems);

        model.addAttribute("products", productPage.getContent());
        model.addAttribute("productPage", productPage);
        model.addAttribute("currentPage", productPage.getNumber());
        model.addAttribute("totalPages", productPage.getTotalPages());
        model.addAttribute("totalItems", totalItems);
        model.addAttribute("startItem", startItem);
        model.addAttribute("endItem", endItem);
        model.addAttribute("pageSize", pageSize);
        model.addAttribute("categories", productCategoryService.getAll());
        model.addAttribute("brands", categoryId == null ? java.util.List.of() : brandCategoryService.getBrandsByCategoryId(categoryId));
        model.addAttribute("applications", vehicleApplicationService.getAll(null, null, null));

        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedCategoryId", categoryId);
        model.addAttribute("selectedBrandId", brandId);
        model.addAttribute("selectedApplicationId", applicationId);
        model.addAttribute("selectedActive", active);
        model.addAttribute("sortBy", selectedSortBy);
        model.addAttribute("sortDir", selectedSortDir);

        return "product/list";
    }

    private Sort buildProductSort(String sortBy, String sortDir) {
        Sort.Direction direction = "desc".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC;
        Sort.Order selectedOrder = new Sort.Order(direction, normalizeProductSortBy(sortBy));

        if ("name".equals(sortBy)) {
            return Sort.by(selectedOrder, Sort.Order.asc("partNumber"));
        }

        return Sort.by(selectedOrder, Sort.Order.asc("name"));
    }

    private String normalizeProductSortBy(String sortBy) {
        if (sortBy == null || sortBy.isBlank()) {
            return "name";
        }

        return switch (sortBy) {
            case "id", "name", "partNumber", "barcode", "position", "dimension", "sku", "createdAt", "updatedAt" -> sortBy;
            default -> "name";
        };
    }

    @GetMapping("/create")
    public String showCreateForm(Model model) {
        ProductCreateRequest form = new ProductCreateRequest();

        model.addAttribute("form", form);
        loadReferences(model, null);
        model.addAttribute("selectedSimilarProduct", null);
        model.addAttribute("pageTitle", "Create Product");
        model.addAttribute("submitUrl", "/products/create");
        model.addAttribute("editMode", false);

        return "product/form";
    }

    @GetMapping("/{id}")
    public String details(@PathVariable Long id, Model model) {
        ProductDetailsResponse details = productService.getDetails(id);
        model.addAttribute("details", details);
        model.addAttribute("product", details.product());
        model.addAttribute("pageTitle", "Product Details");
        return "product/details";
    }

    @PostMapping("/create")
    public String create(@Valid @ModelAttribute("form") ProductCreateRequest request,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            loadReferences(model, request.getCategoryId());
            loadSelectedSimilarProduct(model, request.getSimilarProductId());
            model.addAttribute("pageTitle", "Create Product");
            model.addAttribute("submitUrl", "/products/create");
            model.addAttribute("editMode", false);
            return "product/form";
        }

        try {
            productService.create(request);
            redirectAttributes.addFlashAttribute("successMessage", "Product created successfully.");
            return "redirect:/products";
        } catch (DuplicateResourceException | ResourceNotFoundException | ProductImageException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            loadReferences(model, request.getCategoryId());
            loadSelectedSimilarProduct(model, request.getSimilarProductId());
            model.addAttribute("pageTitle", "Create Product");
            model.addAttribute("submitUrl", "/products/create");
            model.addAttribute("editMode", false);
            return "product/form";
        }
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model) {
        ProductResponse response = productService.getById(id);

        ProductUpdateRequest form = getProductUpdateRequest(response);

        model.addAttribute("productId", id);
        model.addAttribute("hasProductImage", response.hasImage());
        model.addAttribute("form", form);
        loadReferences(model, response.categoryId());
        loadSelectedSimilarProduct(model, response.similarProductId());
        model.addAttribute("pageTitle", "Edit Product");
        model.addAttribute("submitUrl", "/products/" + id + "/edit");
        model.addAttribute("editMode", true);

        return "product/form";
    }

    private static ProductUpdateRequest getProductUpdateRequest(ProductResponse response) {
        ProductUpdateRequest form = new ProductUpdateRequest();
        form.setName(response.name());
        form.setSpecLabel(response.specLabel());
        form.setPosition(response.position());
        form.setDimension(response.dimension());
        form.setSku(response.sku());
        form.setPartNumber(response.partNumber());
        form.setAlternativePartNumber(String.join(", ", response.alternativePartNumbers()));
        form.setBarcode(response.barcode());
        form.setDescription(response.description());
        form.setCategoryId(response.categoryId());
        form.setBrandId(response.brandId());
        form.setApplicationIds(response.applicationIds());
        form.setSimilarProductId(response.similarProductId());
        form.setActive(response.active());
        return form;
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("form") ProductUpdateRequest request,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("productId", id);
            model.addAttribute("hasProductImage", productService.getById(id).hasImage());
            loadReferences(model, request.getCategoryId());
            loadSelectedSimilarProduct(model, request.getSimilarProductId());
            model.addAttribute("pageTitle", "Edit Product");
            model.addAttribute("submitUrl", "/products/" + id + "/edit");
            model.addAttribute("editMode", true);
            return "product/form";
        }

        try {
            productService.update(id, request);
            redirectAttributes.addFlashAttribute("successMessage", "Product updated successfully.");
            return "redirect:/products";
        } catch (DuplicateResourceException | ResourceNotFoundException | ProductImageException ex) {
            model.addAttribute("productId", id);
            model.addAttribute("hasProductImage", productService.getById(id).hasImage());
            model.addAttribute("errorMessage", ex.getMessage());
            loadReferences(model, request.getCategoryId());
            loadSelectedSimilarProduct(model, request.getSimilarProductId());
            model.addAttribute("pageTitle", "Edit Product");
            model.addAttribute("submitUrl", "/products/" + id + "/edit");
            model.addAttribute("editMode", true);
            return "product/form";
        }
    }

    @GetMapping("/search-json")
    @ResponseBody
    public List<ProductSearchItem> searchProductsForCopy(@RequestParam String keyword,
                                                         @RequestParam(required = false) Long categoryId,
                                                         @RequestParam(required = false) Long excludeProductId) {
        if (keyword == null || keyword.trim().length() < 2) {
            return List.of();
        }

        return productService.search(keyword.trim(), categoryId, null, null, true)
                .stream()
                .filter(product -> !product.id().equals(excludeProductId))
                .limit(15)
                .map(product -> new ProductSearchItem(
                        product.id(),
                        buildCopySearchLabel(product)
                ))
                .toList();
    }

    @GetMapping("/{id}/copy-source")
    @ResponseBody
    public ProductCopySource getProductCopySource(@PathVariable Long id) {
        ProductResponse product = productService.getById(id);

        return toProductCopySource(product);
    }

    @GetMapping("/last-copy-source")
    @ResponseBody
    public ResponseEntity<ProductCopySource> getLastProductCopySource() {
        return productService.getLastCreatedProduct()
                .map(this::toProductCopySource)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private ProductCopySource toProductCopySource(ProductResponse product) {
        return new ProductCopySource(
                product.id(),
                product.name(),
                product.specLabel(),
                product.position(),
                product.dimension(),
                product.description(),
                product.categoryId(),
                product.brandId(),
                product.applicationIds(),
                product.active()
        );
    }

    @GetMapping("/{id}/similar-products")
    @ResponseBody
    public List<ProductResponse.SimilarProductSummary> getSimilarityGroup(@PathVariable Long id) {
        return productService.getSimilarityGroup(id);
    }

    @GetMapping("/barcode-suggestion")
    @ResponseBody
    public BarcodeSuggestion suggestBarcode(@RequestParam Long categoryId) {
        return new BarcodeSuggestion(productService.suggestBarcode(categoryId));
    }

    @GetMapping("/{id}/image")
    public ResponseEntity<byte[]> getProductImage(@PathVariable Long id) {
        ProductService.ProductImage image = productService.getImage(id);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noCache())
                .contentType(MediaType.parseMediaType(image.contentType()))
                .body(image.content());
    }

    private String buildCopySearchLabel(ProductResponse product) {
        return java.util.stream.Stream.of(
                        product.categoryName(),
                        product.name(),
                        product.partNumber(),
                        product.position(),
                        product.dimension()
                )
                .filter(value -> value != null && !value.isBlank())
                .collect(java.util.stream.Collectors.joining(" | "));
    }

    public record ProductSearchItem(Long id, String text) {
    }

    public record ProductCopySource(
            Long id,
            String name,
            String specLabel,
            String position,
            String dimension,
            String description,
            Long categoryId,
            Long brandId,
            Set  <Long> applicationIds,
            boolean active
    ) {
    }

    public record BarcodeSuggestion(String barcode) {
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            productService.delete(id);
            redirectAttributes.addFlashAttribute("successMessage", "Product deleted successfully.");
        } catch (ResourceNotFoundException | DuplicateResourceException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/products";
    }

    private void loadReferences(Model model, Long categoryId) {
        model.addAttribute("categories", productCategoryService.getAll());
        model.addAttribute("brands", categoryId == null ? java.util.List.of() : brandCategoryService.getBrandsByCategoryId(categoryId));
        model.addAttribute("applications", vehicleApplicationService.getAll(null, null, null));
        model.addAttribute("vehicleMakes", vehicleMakeService.getAll());
        model.addAttribute("selectedCategoryId", categoryId);
    }

    private void loadSelectedSimilarProduct(Model model, Long similarProductId) {
        if (similarProductId == null) {
            model.addAttribute("selectedSimilarProduct", null);
            return;
        }

        ProductResponse product = productService.getById(similarProductId);
        model.addAttribute("selectedSimilarProduct",
                new ProductResponse.SimilarProductSummary(product.id(), buildCopySearchLabel(product)));
    }
}
