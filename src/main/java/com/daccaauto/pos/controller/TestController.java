package com.daccaauto.pos.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class TestController {

    @GetMapping("/")
    public String index() {
        return "general";
    }

    @GetMapping("/layout")
    public String layout(Model model) {
        return "layout-custom-area";
    }
}
