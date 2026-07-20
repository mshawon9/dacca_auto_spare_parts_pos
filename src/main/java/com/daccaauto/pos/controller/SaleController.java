package com.daccaauto.pos.controller;

import com.daccaauto.pos.dto.sale.*;
import com.daccaauto.pos.entity.SaleType;
import com.daccaauto.pos.entity.VatMode;
import com.daccaauto.pos.entity.PaymentMethod;
import com.daccaauto.pos.exception.DuplicateResourceException;
import com.daccaauto.pos.exception.ResourceNotFoundException;
import com.daccaauto.pos.service.InventoryService;
import com.daccaauto.pos.service.SaleService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/sales")
@RequiredArgsConstructor
public class SaleController {

    private final SaleService saleService;
    private final InventoryService inventoryService;

    @Value("${app.sale.default-vat-percent:5.00}")
    private BigDecimal defaultVatPercent;

    @GetMapping("/create")
    public String create(@RequestParam(required = false) Long draftId, Model model) {
        SaleDraftResponse draft = draftId == null ? null : saleService.getDraft(draftId);
        loadPage(model, draft);
        return "sale/form";
    }

    @PostMapping("/drafts")
    @ResponseBody
    public SaleDraftResponse createDraft() {
        return saleService.createDraft();
    }

    @GetMapping("/drafts")
    @ResponseBody
    public List<SaleDraftSummary> drafts() {
        return saleService.openDrafts();
    }

    @GetMapping("/drafts/{id}")
    @ResponseBody
    public SaleDraftResponse draft(@PathVariable Long id) {
        return saleService.getDraft(id);
    }

    @PostMapping("/drafts/{id}/header")
    @ResponseBody
    public SaleDraftResponse updateHeader(@PathVariable Long id, @RequestBody SaleDraftHeaderRequest request) {
        return saleService.updateHeader(id, request);
    }

    @PostMapping("/drafts/{id}/lines")
    @ResponseBody
    public SaleDraftResponse addLine(@PathVariable Long id, @RequestBody SaleDraftLineRequest request) {
        return saleService.addLine(id, request);
    }

    @DeleteMapping("/drafts/{id}/lines/{lineId}")
    @ResponseBody
    public SaleDraftResponse removeLine(@PathVariable Long id, @PathVariable Long lineId) {
        return saleService.removeLine(id, lineId);
    }

    @DeleteMapping("/drafts/{id}")
    @ResponseBody
    public void deleteDraft(@PathVariable Long id) {
        saleService.deleteDraft(id);
    }

    @PostMapping("/drafts/{id}/complete")
    public String complete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            SaleResponse response = saleService.complete(id);
            redirectAttributes.addFlashAttribute(
                "successMessage",
                "Sale completed. Invoice " + response.invoiceNo() + ", total " + response.total()
            );
            return "redirect:/sales/create";
        } catch (DuplicateResourceException | ResourceNotFoundException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/sales/create?draftId=" + id;
        }
    }

    @GetMapping("/customer-search")
    @ResponseBody
    public List<SelectOption> searchCustomers(@RequestParam String keyword) {
        return saleService.searchCustomers(keyword);
    }

    @GetMapping("/product-search")
    @ResponseBody
    public List<SaleProductOption> searchProducts(@RequestParam String keyword,
                                                  @RequestParam(required = false) Long storeId,
                                                  @RequestParam(required = false) Long customerId) {
        return saleService.searchProducts(keyword, storeId, customerId);
    }

    @GetMapping("/product-info")
    @ResponseBody
    public SaleProductOption productInfo(@RequestParam Long productId,
                                         @RequestParam Long storeId,
                                         @RequestParam(required = false) Long customerId) {
        return saleService.productInfo(productId, storeId, customerId);
    }

    private void loadPage(Model model, SaleDraftResponse draft) {
        model.addAttribute("draft", draft);
        model.addAttribute("newSale", draft == null);
        model.addAttribute("openDrafts", saleService.openDrafts());
        model.addAttribute("stores", inventoryService.getActiveStores());
        model.addAttribute("saleTypes", SaleType.values());
        model.addAttribute("vatModes", VatMode.values());
        model.addAttribute("paymentMethods", PaymentMethod.values());
        model.addAttribute("defaultVatPercent", defaultVatPercent);
        model.addAttribute("saleDate", draft == null ? LocalDate.now() : draft.saleDate());
        model.addAttribute("pageTitle", "Sale Entry");
    }
}
