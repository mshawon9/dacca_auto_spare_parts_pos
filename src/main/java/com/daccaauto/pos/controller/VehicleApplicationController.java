package com.daccaauto.pos.controller;

import com.daccaauto.pos.dto.vehicle.VehicleApplicationCreateRequest;
import com.daccaauto.pos.dto.vehicle.VehicleApplicationResponse;
import com.daccaauto.pos.dto.vehicle.VehicleApplicationUpdateRequest;
import com.daccaauto.pos.exception.DuplicateResourceException;
import com.daccaauto.pos.exception.ResourceNotFoundException;
import com.daccaauto.pos.service.VehicleApplicationService;
import com.daccaauto.pos.service.VehicleMakeService;
import com.daccaauto.pos.service.VehicleModelService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/vehicle-applications")
@RequiredArgsConstructor
public class VehicleApplicationController {

    private final VehicleApplicationService vehicleApplicationService;
    private final VehicleMakeService vehicleMakeService;
    private final VehicleModelService vehicleModelService;

    @GetMapping
    public String list(@RequestParam(required = false) Long makeId,
                       @RequestParam(required = false) Long modelId,
                       @RequestParam(required = false) String keyword,
                       Model model) {

        model.addAttribute("vehicleApplications", vehicleApplicationService.getAll(makeId, modelId, keyword));
        model.addAttribute("vehicleMakes", vehicleMakeService.getAll());
        model.addAttribute("vehicleModels", makeId == null ? List.of() : vehicleModelService.getAll(makeId));

        model.addAttribute("selectedMakeId", makeId);
        model.addAttribute("selectedModelId", modelId);
        model.addAttribute("keyword", keyword);

        return "vehicle-application/list";
    }

    @GetMapping("/create")
    public String showCreateForm(Model model) {
        VehicleApplicationCreateRequest form = new VehicleApplicationCreateRequest();
        model.addAttribute("form", form);
        loadReferenceData(model, null);
        model.addAttribute("pageTitle", "Create Vehicle Application");
        model.addAttribute("submitUrl", "/vehicle-applications/create");
        model.addAttribute("editMode", false);
        return "vehicle-application/form";
    }

    @PostMapping("/create")
    public String create(@Valid @ModelAttribute("form") VehicleApplicationCreateRequest request,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            loadReferenceData(model, request.getVehicleMakeId());
            model.addAttribute("pageTitle", "Create Vehicle Application");
            model.addAttribute("submitUrl", "/vehicle-applications/create");
            model.addAttribute("editMode", false);
            return "vehicle-application/form";
        }

        try {
            vehicleApplicationService.create(request);
            redirectAttributes.addFlashAttribute("successMessage", "Vehicle application created successfully.");
            return "redirect:/vehicle-applications";
        } catch (DuplicateResourceException | ResourceNotFoundException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            loadReferenceData(model, request.getVehicleMakeId());
            model.addAttribute("pageTitle", "Create Vehicle Application");
            model.addAttribute("submitUrl", "/vehicle-applications/create");
            model.addAttribute("editMode", false);
            return "vehicle-application/form";
        }
    }

    @PostMapping("/create-json")
    @ResponseBody
    public VehicleApplicationResponse createJson(@Valid @RequestBody VehicleApplicationCreateRequest request) {
        return vehicleApplicationService.create(request);
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model) {
        VehicleApplicationResponse response = vehicleApplicationService.getById(id);

        VehicleApplicationUpdateRequest form = new VehicleApplicationUpdateRequest();
        form.setVehicleMakeId(response.vehicleMakeId());
        form.setVehicleModelId(response.vehicleModelId());
        form.setVehicleMakeName(response.vehicleMakeName());
        form.setVehicleModelName(response.vehicleModelName());
        form.setVariantLabel(response.variantLabel());
        form.setYearFrom(response.yearFrom());
        form.setYearTo(response.yearTo());
        form.setActive(response.active());

        model.addAttribute("vehicleApplicationId", id);
        model.addAttribute("form", form);
        loadReferenceData(model, response.vehicleMakeId());
        model.addAttribute("pageTitle", "Edit Vehicle Application");
        model.addAttribute("submitUrl", "/vehicle-applications/" + id + "/edit");
        model.addAttribute("editMode", true);
        return "vehicle-application/form";
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("form") VehicleApplicationUpdateRequest request,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("vehicleApplicationId", id);
            loadReferenceData(model, request.getVehicleMakeId());
            model.addAttribute("pageTitle", "Edit Vehicle Application");
            model.addAttribute("submitUrl", "/vehicle-applications/" + id + "/edit");
            model.addAttribute("editMode", true);
            return "vehicle-application/form";
        }

        try {
            vehicleApplicationService.update(id, request);
            redirectAttributes.addFlashAttribute("successMessage", "Vehicle application updated successfully.");
            return "redirect:/vehicle-applications";
        } catch (DuplicateResourceException | ResourceNotFoundException ex) {
            model.addAttribute("vehicleApplicationId", id);
            model.addAttribute("errorMessage", ex.getMessage());
            loadReferenceData(model, request.getVehicleMakeId());
            model.addAttribute("pageTitle", "Edit Vehicle Application");
            model.addAttribute("submitUrl", "/vehicle-applications/" + id + "/edit");
            model.addAttribute("editMode", true);
            return "vehicle-application/form";
        }
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            vehicleApplicationService.delete(id);
            redirectAttributes.addFlashAttribute("successMessage", "Vehicle application deleted successfully.");
        } catch (ResourceNotFoundException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/vehicle-applications";
    }

    private void loadReferenceData(Model model, Long selectedMakeId) {
        model.addAttribute("vehicleMakes", vehicleMakeService.getAll());
        model.addAttribute("vehicleModels", selectedMakeId == null ? List.of() : vehicleModelService.getAll(selectedMakeId));
        model.addAttribute("selectedMakeId", selectedMakeId);
    }
}
