package com.daccaauto.pos.controller;

import com.daccaauto.pos.dto.brand.BrandResponse;
import com.daccaauto.pos.service.BrandCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/lookups")
@RequiredArgsConstructor
public class LookupApiController {

    private final BrandCategoryService brandCategoryService;

    @GetMapping("/brands")
    public List<BrandResponse> getBrandsByCategory(@RequestParam Long categoryId) {
        return brandCategoryService.getBrandsByCategoryId(categoryId);
    }
}