package com.daccaauto.pos.controller;

import com.daccaauto.pos.dto.category.ProductCategoryCreateRequest;
import com.daccaauto.pos.dto.category.ProductCategoryResponse;
import com.daccaauto.pos.dto.category.ProductCategoryUpdateRequest;
import com.daccaauto.pos.exception.DuplicateResourceException;
import com.daccaauto.pos.exception.ResourceNotFoundException;
import com.daccaauto.pos.service.BrandCategoryService;
import com.daccaauto.pos.service.BrandService;
import com.daccaauto.pos.service.ProductCategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.LinkedHashSet;

@Controller
@RequestMapping("/categories")
@RequiredArgsConstructor
public class ProductCategoryController {

    private final ProductCategoryService productCategoryService;
    private final BrandService brandService;
    private final BrandCategoryService brandCategoryService;

    @GetMapping
    public String page(@RequestParam(required = false) Long brandId, Model model) {
        prepareListPage(
                model,
                brandId,
                new ProductCategoryCreateRequest(),
                "Create Category",
                "/categories/create",
                false,
                false
        );
        return "category/list";
    }

    @GetMapping("/{id}")
    public String details(@PathVariable Long id, Model model) {
        ProductCategoryResponse category = productCategoryService.getById(id);

        model.addAttribute("category", category);
        model.addAttribute("mappedBrands", brandCategoryService.getBrandsByCategoryId(id));

        return "category/details";
    }

    @PostMapping("/create")
    public String create(@Valid @ModelAttribute("form") ProductCategoryCreateRequest request,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            prepareListPage(
                    model,
                    null,
                    request,
                    "Create Category",
                    "/categories/create",
                    false,
                    true
            );
            return "category/list";
        }

        try {
            productCategoryService.create(request);
            redirectAttributes.addFlashAttribute("successMessage", "Category created successfully.");
            return "redirect:/categories";
        } catch (DuplicateResourceException | ResourceNotFoundException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            prepareListPage(
                    model,
                    null,
                    request,
                    "Create Category",
                    "/categories/create",
                    false,
                    true
            );
            return "category/list";
        }
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("form") ProductCategoryUpdateRequest request,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("editingCategoryId", id);
            prepareListPage(
                    model,
                    null,
                    request,
                    "Edit Category",
                    "/categories/" + id + "/edit",
                    true,
                    true
            );
            return "category/list";
        }

        try {
            productCategoryService.update(id, request);
            redirectAttributes.addFlashAttribute("successMessage", "Category updated successfully.");
            return "redirect:/categories";
        } catch (DuplicateResourceException | ResourceNotFoundException ex) {
            model.addAttribute("editingCategoryId", id);
            model.addAttribute("errorMessage", ex.getMessage());
            prepareListPage(
                    model,
                    null,
                    request,
                    "Edit Category",
                    "/categories/" + id + "/edit",
                    true,
                    true
            );
            return "category/list";
        }
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            productCategoryService.delete(id);
            redirectAttributes.addFlashAttribute("successMessage", "Category deleted successfully.");
        } catch (ResourceNotFoundException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/categories";
    }

    @GetMapping("/{id}/edit-data")
    @ResponseBody
    public ProductCategoryUpdateRequest getEditData(@PathVariable Long id) {
        ProductCategoryResponse category = productCategoryService.getById(id);

        ProductCategoryUpdateRequest form = new ProductCategoryUpdateRequest();
        form.setName(category.name());
        form.setDescription(category.description());
        form.setActive(category.active());
        form.setBrandIds(new LinkedHashSet<>(
                brandCategoryService.getBrandsByCategoryId(id)
                        .stream()
                        .map(brand -> brand.id())
                        .toList()
        ));

        return form;
    }

    private void prepareListPage(Model model,
                                 Long selectedBrandId,
                                 Object form,
                                 String modalTitle,
                                 String modalAction,
                                 boolean editMode,
                                 boolean showModal) {

        model.addAttribute("categories", productCategoryService.getAllFilteredByBrand(selectedBrandId));
        model.addAttribute("brands", brandService.getAll());
        model.addAttribute("selectedBrandId", selectedBrandId);

        model.addAttribute("form", form);
        model.addAttribute("modalTitle", modalTitle);
        model.addAttribute("modalAction", modalAction);
        model.addAttribute("editMode", editMode);
        model.addAttribute("showCategoryModal", showModal);
    }
}