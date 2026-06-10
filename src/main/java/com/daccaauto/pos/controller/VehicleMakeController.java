package com.daccaauto.pos.controller;

import com.daccaauto.pos.dto.vehicle.VehicleMakeCreateRequest;
import com.daccaauto.pos.dto.vehicle.VehicleMakeUpdateRequest;
import com.daccaauto.pos.exception.DuplicateResourceException;
import com.daccaauto.pos.exception.ResourceNotFoundException;
import com.daccaauto.pos.service.VehicleMakeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/vehicle-makes")
@RequiredArgsConstructor
public class VehicleMakeController {

    private final VehicleMakeService vehicleMakeService;

    @GetMapping
    public String page(Model model) {
        preparePage(
            model,
            new VehicleMakeCreateRequest(),
            "Create Vehicle Make",
            "/vehicle-makes/create",
            false,
            false
        );
        return "vehicle-make/list";
    }

    @PostMapping("/create")
    public String create(@Valid @ModelAttribute("form") VehicleMakeCreateRequest request,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            preparePage(model, request, "Create Vehicle Make", "/vehicle-makes/create", false, true);
            return "vehicle-make/list";
        }

        try {
            vehicleMakeService.create(request);
            redirectAttributes.addFlashAttribute("successMessage", "Vehicle make created successfully.");
            return "redirect:/vehicle-makes";
        } catch (DuplicateResourceException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            preparePage(model, request, "Create Vehicle Make", "/vehicle-makes/create", false, true);
            return "vehicle-make/list";
        }
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("form") VehicleMakeUpdateRequest request,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("editingVehicleMakeId", id);
            preparePage(model, request, "Edit Vehicle Make", "/vehicle-makes/" + id + "/edit", true, true);
            return "vehicle-make/list";
        }

        try {
            vehicleMakeService.update(id, request);
            redirectAttributes.addFlashAttribute("successMessage", "Vehicle make updated successfully.");
            return "redirect:/vehicle-makes";
        } catch (DuplicateResourceException | ResourceNotFoundException ex) {
            model.addAttribute("editingVehicleMakeId", id);
            model.addAttribute("errorMessage", ex.getMessage());
            preparePage(model, request, "Edit Vehicle Make", "/vehicle-makes/" + id + "/edit", true, true);
            return "vehicle-make/list";
        }
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            vehicleMakeService.delete(id);
            redirectAttributes.addFlashAttribute("successMessage", "Vehicle make deleted successfully.");
        } catch (ResourceNotFoundException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/vehicle-makes";
    }

    private void preparePage(Model model,
                             Object form,
                             String modalTitle,
                             String modalAction,
                             boolean editMode,
                             boolean showModal) {
        model.addAttribute("vehicleMakes", vehicleMakeService.getAll());
        model.addAttribute("form", form);
        model.addAttribute("modalTitle", modalTitle);
        model.addAttribute("modalAction", modalAction);
        model.addAttribute("editMode", editMode);
        model.addAttribute("showVehicleMakeModal", showModal);
    }
}