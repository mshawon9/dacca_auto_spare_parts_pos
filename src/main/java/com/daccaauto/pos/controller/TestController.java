package com.daccaauto.pos.controller;

import com.daccaauto.pos.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class TestController {

    private final DashboardService dashboardService;

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("dashboard", dashboardService.getSummary());
        return "general";
    }

    @GetMapping("/layout")
    public String layout(Model model) {
        return "layout-custom-area";
    }
}
