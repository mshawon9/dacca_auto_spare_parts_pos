package com.daccaauto.pos.controller;

import com.daccaauto.pos.dto.supplier.SupplierCreateRequest;
import com.daccaauto.pos.dto.supplier.SupplierUpdateRequest;
import com.daccaauto.pos.exception.DuplicateResourceException;
import com.daccaauto.pos.service.SupplierService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/suppliers")
@RequiredArgsConstructor
public class SupplierController {

    private static final int PAGE_SIZE = 15;

    private final SupplierService supplierService;

    @GetMapping
    public String list(@RequestParam(defaultValue = "0") int page, Model model) {
        loadList(model, Math.max(page, 0));
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", new SupplierCreateRequest());
        }
        model.addAttribute("modalAction", "/suppliers");
        model.addAttribute("modalTitle", "Add Supplier");
        model.addAttribute("submitLabel", "Add Supplier");
        model.addAttribute("openCreateModal", model.containsAttribute("openCreateModal"));
        model.addAttribute("pageTitle", "Suppliers");
        return "supplier/list";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("form") SupplierCreateRequest request,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            loadList(model, 0);
            model.addAttribute("modalAction", "/suppliers");
            model.addAttribute("modalTitle", "Add Supplier");
            model.addAttribute("submitLabel", "Add Supplier");
            model.addAttribute("openCreateModal", true);
            model.addAttribute("pageTitle", "Suppliers");
            return "supplier/list";
        }

        try {
            supplierService.create(request);
            redirectAttributes.addFlashAttribute("successMessage", "Supplier added successfully.");
            return "redirect:/suppliers";
        } catch (DuplicateResourceException ex) {
            loadList(model, 0);
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("modalAction", "/suppliers");
            model.addAttribute("modalTitle", "Add Supplier");
            model.addAttribute("submitLabel", "Add Supplier");
            model.addAttribute("openCreateModal", true);
            model.addAttribute("pageTitle", "Suppliers");
            return "supplier/list";
        }
    }

    @PostMapping("/{id}/edit")
    public String edit(@PathVariable Long id,
                       @Valid @ModelAttribute("form") SupplierCreateRequest request,
                       BindingResult bindingResult,
                       Model model,
                       RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            loadList(model, 0);
            model.addAttribute("modalAction", "/suppliers/" + id + "/edit");
            model.addAttribute("modalTitle", "Edit Supplier");
            model.addAttribute("submitLabel", "Update Supplier");
            model.addAttribute("openCreateModal", true);
            model.addAttribute("pageTitle", "Suppliers");
            return "supplier/list";
        }

        try {
            supplierService.update(id, toUpdateRequest(request));
            redirectAttributes.addFlashAttribute("successMessage", "Supplier updated successfully.");
            return "redirect:/suppliers";
        } catch (DuplicateResourceException ex) {
            loadList(model, 0);
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("modalAction", "/suppliers/" + id + "/edit");
            model.addAttribute("modalTitle", "Edit Supplier");
            model.addAttribute("submitLabel", "Update Supplier");
            model.addAttribute("openCreateModal", true);
            model.addAttribute("pageTitle", "Suppliers");
            return "supplier/list";
        }
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        supplierService.delete(id);
        redirectAttributes.addFlashAttribute("successMessage", "Supplier deleted successfully.");
        return "redirect:/suppliers";
    }

    private void loadList(Model model, int page) {
        var supplierPage = supplierService.getPage(PageRequest.of(page, PAGE_SIZE));
        model.addAttribute("supplierPage", supplierPage);
        model.addAttribute("suppliers", supplierPage.getContent());
    }

    private SupplierUpdateRequest toUpdateRequest(SupplierCreateRequest request) {
        SupplierUpdateRequest updateRequest = new SupplierUpdateRequest();
        updateRequest.setName(request.getName());
        updateRequest.setContactPerson(request.getContactPerson());
        updateRequest.setPhone(request.getPhone());
        updateRequest.setEmail(request.getEmail());
        updateRequest.setAddress(request.getAddress());
        updateRequest.setTrnNumber(request.getTrnNumber());
        updateRequest.setActive(request.getActive());
        return updateRequest;
    }
}
