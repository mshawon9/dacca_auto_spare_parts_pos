package com.daccaauto.pos.controller;

import com.daccaauto.pos.dto.brand.BrandResponse;
import com.daccaauto.pos.dto.vehicle.VehicleModelResponse;
import com.daccaauto.pos.service.BrandCategoryService;
import com.daccaauto.pos.service.VehicleModelService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/lookups")
@RequiredArgsConstructor
public class LookupApiController {

    private final BrandCategoryService brandCategoryService;

    private final VehicleModelService vehicleModelService;

    @GetMapping("/brands")
    public List<BrandResponse> getBrandsByCategory(@RequestParam Long categoryId) {
        return brandCategoryService.getBrandsByCategoryId(categoryId);
    }

    @GetMapping("/vehicle-models")
    public List<VehicleModelResponse> getVehicleModelsByMake(@RequestParam Long makeId) {
        return vehicleModelService.getAll(makeId);
    }


}