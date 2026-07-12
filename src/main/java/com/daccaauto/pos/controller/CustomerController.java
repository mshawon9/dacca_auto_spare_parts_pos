package com.daccaauto.pos.controller;

import com.daccaauto.pos.dto.customer.CustomerCreateRequest;
import com.daccaauto.pos.exception.DuplicateResourceException;
import com.daccaauto.pos.service.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/customers")
@RequiredArgsConstructor
public class CustomerController {

    private static final int PAGE_SIZE = 15;

    private final CustomerService customerService;

    @GetMapping
    public String list(@RequestParam(defaultValue = "0") int page, Model model) {
        loadList(model, Math.max(page, 0));
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", new CustomerCreateRequest());
        }
        model.addAttribute("modalAction", "/customers");
        model.addAttribute("modalTitle", "Add Customer");
        model.addAttribute("submitLabel", "Add Customer");
        model.addAttribute("openCreateModal", model.containsAttribute("openCreateModal"));
        model.addAttribute("pageTitle", "Customers");
        return "customer/list";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("form") CustomerCreateRequest request,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            loadList(model, 0);
            model.addAttribute("modalAction", "/customers");
            model.addAttribute("modalTitle", "Add Customer");
            model.addAttribute("submitLabel", "Add Customer");
            model.addAttribute("openCreateModal", true);
            model.addAttribute("pageTitle", "Customers");
            return "customer/list";
        }

        try {
            customerService.create(request);
            redirectAttributes.addFlashAttribute("successMessage", "Customer added successfully.");
            return "redirect:/customers";
        } catch (DuplicateResourceException ex) {
            loadList(model, 0);
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("modalAction", "/customers");
            model.addAttribute("modalTitle", "Add Customer");
            model.addAttribute("submitLabel", "Add Customer");
            model.addAttribute("openCreateModal", true);
            model.addAttribute("pageTitle", "Customers");
            return "customer/list";
        }
    }

    @PostMapping("/{id}/edit")
    public String edit(@PathVariable Long id,
                       @Valid @ModelAttribute("form") CustomerCreateRequest request,
                       BindingResult bindingResult,
                       Model model,
                       RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            loadList(model, 0);
            model.addAttribute("modalAction", "/customers/" + id + "/edit");
            model.addAttribute("modalTitle", "Edit Customer");
            model.addAttribute("submitLabel", "Update Customer");
            model.addAttribute("openCreateModal", true);
            model.addAttribute("pageTitle", "Customers");
            return "customer/list";
        }

        try {
            customerService.update(id, request);
            redirectAttributes.addFlashAttribute("successMessage", "Customer updated successfully.");
            return "redirect:/customers";
        } catch (DuplicateResourceException ex) {
            loadList(model, 0);
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("modalAction", "/customers/" + id + "/edit");
            model.addAttribute("modalTitle", "Edit Customer");
            model.addAttribute("submitLabel", "Update Customer");
            model.addAttribute("openCreateModal", true);
            model.addAttribute("pageTitle", "Customers");
            return "customer/list";
        }
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        customerService.delete(id);
        redirectAttributes.addFlashAttribute("successMessage", "Customer deleted successfully.");
        return "redirect:/customers";
    }

    private void loadList(Model model, int page) {
        var customerPage = customerService.getPage(PageRequest.of(page, PAGE_SIZE));
        model.addAttribute("customerPage", customerPage);
        model.addAttribute("customers", customerPage.getContent());
    }
}
