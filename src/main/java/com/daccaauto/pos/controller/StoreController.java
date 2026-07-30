package com.daccaauto.pos.controller;

import com.daccaauto.pos.dto.store.StoreManageRequest;
import com.daccaauto.pos.exception.DuplicateResourceException;
import com.daccaauto.pos.exception.ResourceNotFoundException;
import com.daccaauto.pos.service.StoreService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.util.UriUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.nio.charset.StandardCharsets;

@Controller
@RequestMapping("/stores")
@RequiredArgsConstructor
public class StoreController {

    private static final int PAGE_SIZE = 15;

    private final StoreService storeService;

    @GetMapping
    public String list(@RequestParam(required = false) String keyword,
                       @RequestParam(defaultValue = "0") int page,
                       Model model) {
        loadList(model, keyword, Math.max(page, 0));
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", new StoreManageRequest());
        }
        prepareModal(model, "/stores", "Add Store / Shop", "Add Store");
        model.addAttribute("openStoreModal", model.containsAttribute("openStoreModal"));
        return "store/list";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("form") StoreManageRequest request,
                         BindingResult bindingResult,
                         @RequestParam(required = false) String keyword,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return formError(model, keyword, request, "/stores", "Add Store / Shop", "Add Store");
        }

        try {
            storeService.create(request);
            redirectAttributes.addFlashAttribute("successMessage", "Store added successfully.");
            return redirectToList(keyword);
        } catch (DuplicateResourceException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            return formError(model, keyword, request, "/stores", "Add Store / Shop", "Add Store");
        }
    }

    @PostMapping("/{id}/edit")
    public String edit(@PathVariable Long id,
                       @Valid @ModelAttribute("form") StoreManageRequest request,
                       BindingResult bindingResult,
                       @RequestParam(required = false) String keyword,
                       Model model,
                       RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return formError(model, keyword, request, "/stores/" + id + "/edit", "Edit Store / Shop", "Update Store");
        }

        try {
            storeService.update(id, request);
            redirectAttributes.addFlashAttribute("successMessage", "Store updated successfully.");
            return redirectToList(keyword);
        } catch (DuplicateResourceException | ResourceNotFoundException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            return formError(model, keyword, request, "/stores/" + id + "/edit", "Edit Store / Shop", "Update Store");
        }
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id,
                         @RequestParam(required = false) String keyword,
                         RedirectAttributes redirectAttributes) {
        try {
            storeService.delete(id);
            redirectAttributes.addFlashAttribute("successMessage", "Store deleted successfully.");
        } catch (DuplicateResourceException | ResourceNotFoundException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return redirectToList(keyword);
    }

    private String formError(Model model,
                             String keyword,
                             StoreManageRequest request,
                             String modalAction,
                             String modalTitle,
                             String submitLabel) {
        loadList(model, keyword, 0);
        model.addAttribute("form", request);
        prepareModal(model, modalAction, modalTitle, submitLabel);
        model.addAttribute("openStoreModal", true);
        return "store/list";
    }

    private void loadList(Model model, String keyword, int page) {
        var storePage = storeService.getPage(keyword, PageRequest.of(page, PAGE_SIZE));
        model.addAttribute("storePage", storePage);
        model.addAttribute("stores", storePage.getContent());
        model.addAttribute("keyword", keyword);
    }

    private void prepareModal(Model model, String modalAction, String modalTitle, String submitLabel) {
        model.addAttribute("modalAction", modalAction);
        model.addAttribute("modalTitle", modalTitle);
        model.addAttribute("submitLabel", submitLabel);
    }

    private String redirectToList(String keyword) {
        return keyword == null || keyword.isBlank()
            ? "redirect:/stores"
            : "redirect:/stores?keyword=" + UriUtils.encodeQueryParam(keyword.trim(), StandardCharsets.UTF_8);
    }
}
