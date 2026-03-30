package com.daccaauto.pos.controller;

import com.daccaauto.pos.dto.brand.BrandCreateRequest;
import com.daccaauto.pos.dto.brand.BrandUpdateRequest;
import com.daccaauto.pos.exception.DuplicateResourceException;
import com.daccaauto.pos.exception.ResourceNotFoundException;
import com.daccaauto.pos.service.BrandService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/brands")
@RequiredArgsConstructor
public class BrandController {

    private final BrandService brandService;

    @GetMapping
    public String page(Model model) {
        preparePage(
                model,
                new BrandCreateRequest(),
                "Create Brand",
                "/brands/create",
                false,
                false
        );
        return "brand/list";
    }

    @PostMapping("/create")
    public String create(@Valid @ModelAttribute("form") BrandCreateRequest request,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            preparePage(
                    model,
                    request,
                    "Create Brand",
                    "/brands/create",
                    false,
                    true
            );
            return "brand/list";
        }

        try {
            brandService.create(request);
            redirectAttributes.addFlashAttribute("successMessage", "Brand created successfully.");
            return "redirect:/brands";
        } catch (DuplicateResourceException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            preparePage(
                    model,
                    request,
                    "Create Brand",
                    "/brands/create",
                    false,
                    true
            );
            return "brand/list";
        }
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("form") BrandUpdateRequest request,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            preparePage(
                    model,
                    request,
                    "Edit Brand",
                    "/brands/" + id + "/edit",
                    true,
                    true
            );
            model.addAttribute("editingBrandId", id);
            return "brand/list";
        }

        try {
            brandService.update(id, request);
            redirectAttributes.addFlashAttribute("successMessage", "Brand updated successfully.");
            return "redirect:/brands";
        } catch (DuplicateResourceException | ResourceNotFoundException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            preparePage(
                    model,
                    request,
                    "Edit Brand",
                    "/brands/" + id + "/edit",
                    true,
                    true
            );
            model.addAttribute("editingBrandId", id);
            return "brand/list";
        }
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            brandService.delete(id);
            redirectAttributes.addFlashAttribute("successMessage", "Brand deleted successfully.");
        } catch (ResourceNotFoundException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/brands";
    }

    private void preparePage(Model model,
                             Object form,
                             String modalTitle,
                             String modalAction,
                             boolean editMode,
                             boolean showModal) {

        model.addAttribute("brands", brandService.getAll());
        model.addAttribute("form", form);
        model.addAttribute("modalTitle", modalTitle);
        model.addAttribute("modalAction", modalAction);
        model.addAttribute("editMode", editMode);
        model.addAttribute("showBrandModal", showModal);
    }
}