package com.daccaauto.pos.controller;

import com.daccaauto.pos.dto.vehicle.VehicleModelCreateRequest;
import com.daccaauto.pos.dto.vehicle.VehicleModelUpdateRequest;
import com.daccaauto.pos.exception.DuplicateResourceException;
import com.daccaauto.pos.exception.ResourceNotFoundException;
import com.daccaauto.pos.service.VehicleMakeService;
import com.daccaauto.pos.service.VehicleModelService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/vehicle-models")
@RequiredArgsConstructor
public class VehicleModelController {

    private final VehicleModelService vehicleModelService;
    private final VehicleMakeService vehicleMakeService;

    @GetMapping
    public String page(@RequestParam(required = false) Long makeId, Model model) {
        preparePage(
                model,
                makeId,
                new VehicleModelCreateRequest(),
                "Create Vehicle Model",
                "/vehicle-models/create",
                false,
                false
        );
        return "vehicle-model/list";
    }

    @PostMapping("/create")
    public String create(@Valid @ModelAttribute("form") VehicleModelCreateRequest request,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            preparePage(model, request.getMakeId(), request, "Create Vehicle Model", "/vehicle-models/create", false, true);
            return "vehicle-model/list";
        }

        try {
            vehicleModelService.create(request);
            redirectAttributes.addFlashAttribute("successMessage", "Vehicle model created successfully.");
            return "redirect:/vehicle-models";
        } catch (DuplicateResourceException | ResourceNotFoundException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            preparePage(model, request.getMakeId(), request, "Create Vehicle Model", "/vehicle-models/create", false, true);
            return "vehicle-model/list";
        }
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("form") VehicleModelUpdateRequest request,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("editingVehicleModelId", id);
            preparePage(model, request.getMakeId(), request, "Edit Vehicle Model", "/vehicle-models/" + id + "/edit", true, true);
            return "vehicle-model/list";
        }

        try {
            vehicleModelService.update(id, request);
            redirectAttributes.addFlashAttribute("successMessage", "Vehicle model updated successfully.");
            return "redirect:/vehicle-models";
        } catch (DuplicateResourceException | ResourceNotFoundException ex) {
            model.addAttribute("editingVehicleModelId", id);
            model.addAttribute("errorMessage", ex.getMessage());
            preparePage(model, request.getMakeId(), request, "Edit Vehicle Model", "/vehicle-models/" + id + "/edit", true, true);
            return "vehicle-model/list";
        }
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            vehicleModelService.delete(id);
            redirectAttributes.addFlashAttribute("successMessage", "Vehicle model deleted successfully.");
        } catch (ResourceNotFoundException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/vehicle-models";
    }

    private void preparePage(Model model,
                             Long selectedMakeId,
                             Object form,
                             String modalTitle,
                             String modalAction,
                             boolean editMode,
                             boolean showModal) {
        model.addAttribute("vehicleMakes", vehicleMakeService.getAll());
        model.addAttribute("vehicleModels", vehicleModelService.getAll(selectedMakeId));
        model.addAttribute("selectedMakeId", selectedMakeId);

        model.addAttribute("form", form);
        model.addAttribute("modalTitle", modalTitle);
        model.addAttribute("modalAction", modalAction);
        model.addAttribute("editMode", editMode);
        model.addAttribute("showVehicleModelModal", showModal);
    }
}