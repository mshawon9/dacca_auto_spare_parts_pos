package com.daccaauto.pos.controller;

import com.daccaauto.pos.dto.purchase.PurchaseOrderRequest;
import com.daccaauto.pos.dto.purchase.PurchaseReturnRequest;
import com.daccaauto.pos.exception.DuplicateResourceException;
import com.daccaauto.pos.exception.ResourceNotFoundException;
import com.daccaauto.pos.repository.ProductSupplierRepository;
import com.daccaauto.pos.service.InventoryService;
import com.daccaauto.pos.service.ProductService;
import com.daccaauto.pos.service.PurchaseOrderService;
import com.daccaauto.pos.service.SupplierService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.ArrayList;

@Controller
@RequestMapping("/purchase-orders")
@RequiredArgsConstructor
public class PurchaseOrderController {

    private static final int PAGE_SIZE = 15;

    private final PurchaseOrderService purchaseOrderService;
    private final SupplierService supplierService;
    private final InventoryService inventoryService;
    private final ProductService productService;
    private final ProductSupplierRepository productSupplierRepository;

    @Value("${app.purchase.default-tax-percent:5.00}")
    private java.math.BigDecimal defaultTaxPercent;

    @GetMapping
    public String list(@RequestParam(required = false) String keyword,
                       @RequestParam(defaultValue = "0") int page,
                       Model model) {
        var purchaseOrderPage = purchaseOrderService.search(keyword, PageRequest.of(Math.max(page, 0), PAGE_SIZE));
        model.addAttribute("purchaseOrderPage", purchaseOrderPage);
        model.addAttribute("purchaseOrders", purchaseOrderPage.getContent());
        model.addAttribute("keyword", keyword);
        model.addAttribute("pageTitle", "Purchase Orders");
        return "purchase-order/list";
    }

    @GetMapping("/create")
    public String create(Model model) {
        if (!model.containsAttribute("form")) {
            PurchaseOrderRequest form = new PurchaseOrderRequest();
            form.setPurchaseDate(LocalDate.now());
            form.setLines(new ArrayList<>());
            model.addAttribute("form", form);
        }
        loadReferences(model);
        model.addAttribute("pageTitle", "Purchase Order Entry");
        model.addAttribute("formAction", "/purchase-orders");
        model.addAttribute("formTitle", "Purchase Order Entry");
        model.addAttribute("submitLabel", "Save & Update Stock");
        return "purchase-order/form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            model.addAttribute("form", purchaseOrderService.getForm(id));
            loadReferences(model);
            model.addAttribute("pageTitle", "Edit Purchase Order");
            model.addAttribute("formAction", "/purchase-orders/" + id + "/edit");
            model.addAttribute("formTitle", "Edit Purchase Order");
            model.addAttribute("submitLabel", "Update PO & Stock");
            return "purchase-order/form";
        } catch (DuplicateResourceException | ResourceNotFoundException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/purchase-orders";
        }
    }

    @PostMapping
    public String save(@Valid @ModelAttribute("form") PurchaseOrderRequest request,
                       BindingResult bindingResult,
                       Model model,
                       RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            loadReferences(model);
            model.addAttribute("errorMessage", "Please check purchase order details.");
            model.addAttribute("pageTitle", "Purchase Order Entry");
            model.addAttribute("formAction", "/purchase-orders");
            model.addAttribute("formTitle", "Purchase Order Entry");
            model.addAttribute("submitLabel", "Save & Update Stock");
            return "purchase-order/form";
        }

        try {
            var response = purchaseOrderService.create(request);
            redirectAttributes.addFlashAttribute(
                "successMessage",
                "Purchase saved. Invoice " + response.invoiceId() + ", total " + response.total()
            );
            return "redirect:/purchase-orders/create";
        } catch (DuplicateResourceException | ResourceNotFoundException ex) {
            loadReferences(model);
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("pageTitle", "Purchase Order Entry");
            model.addAttribute("formAction", "/purchase-orders");
            model.addAttribute("formTitle", "Purchase Order Entry");
            model.addAttribute("submitLabel", "Save & Update Stock");
            return "purchase-order/form";
        }
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("form") PurchaseOrderRequest request,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            loadReferences(model);
            model.addAttribute("errorMessage", "Please check purchase order details.");
            model.addAttribute("pageTitle", "Edit Purchase Order");
            model.addAttribute("formAction", "/purchase-orders/" + id + "/edit");
            model.addAttribute("formTitle", "Edit Purchase Order");
            model.addAttribute("submitLabel", "Update PO & Stock");
            return "purchase-order/form";
        }

        try {
            var response = purchaseOrderService.update(id, request);
            redirectAttributes.addFlashAttribute(
                "successMessage",
                "Purchase order updated. Invoice " + response.invoiceId() + ", total " + response.total()
            );
            return "redirect:/purchase-orders";
        } catch (DuplicateResourceException | ResourceNotFoundException ex) {
            loadReferences(model);
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("pageTitle", "Edit Purchase Order");
            model.addAttribute("formAction", "/purchase-orders/" + id + "/edit");
            model.addAttribute("formTitle", "Edit Purchase Order");
            model.addAttribute("submitLabel", "Update PO & Stock");
            return "purchase-order/form";
        }
    }

    @GetMapping("/{id}/detail-json")
    @ResponseBody
    public Object detailJson(@PathVariable Long id) {
        return purchaseOrderService.getDetail(id);
    }

    @PostMapping("/{id}/return")
    public String returnItem(@PathVariable Long id,
                             @Valid @ModelAttribute PurchaseReturnRequest request,
                             RedirectAttributes redirectAttributes) {
        try {
            purchaseOrderService.returnItem(id, request);
            redirectAttributes.addFlashAttribute("successMessage", "Purchase item returned and stock updated.");
        } catch (DuplicateResourceException | ResourceNotFoundException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/purchase-orders";
    }

    @GetMapping("/supplier-product-code")
    @ResponseBody
    public SupplierProductCode getSupplierProductCode(@RequestParam Long supplierId,
                                                      @RequestParam Long productId) {
        return productSupplierRepository.findByProductIdAndSupplierId(productId, supplierId)
            .map(mapping -> new SupplierProductCode(
                mapping.getSupplierProductCode(),
                mapping.getPriceValue()
            ))
            .orElseGet(() -> new SupplierProductCode(null, null));
    }

    @GetMapping("/product-search")
    @ResponseBody
    public java.util.List<ProductOption> searchProducts(@RequestParam String keyword) {
        if (keyword == null || keyword.trim().length() < 2) {
            return java.util.List.of();
        }

        return productService.search(keyword.trim(), null, null, null, true)
            .stream()
            .limit(20)
            .map(product -> new ProductOption(
                product.id(),
                java.util.stream.Stream.of(
                        product.categoryName(),
                        product.name(),
                        "Brand: " + product.brandName(),
                        "Part: " + product.partNumber()
                    )
                    .filter(value -> value != null && !value.isBlank())
                    .collect(java.util.stream.Collectors.joining(" | "))
            ))
            .toList();
    }


    private void loadReferences(Model model) {
        model.addAttribute("suppliers", supplierService.getAll());
        model.addAttribute("stores", inventoryService.getActiveStores());
        model.addAttribute("defaultTaxPercent", defaultTaxPercent);
    }

    public record SupplierProductCode(String supplierProductCode, java.math.BigDecimal priceValue) {
    }

    public record ProductOption(Long id, String text) {
    }
}
