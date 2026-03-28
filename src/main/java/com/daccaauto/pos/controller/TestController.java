package com.daccaauto.pos.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class TestController {
    @GetMapping("/")
    public String index() {
        return "general";
    }

    @GetMapping("/layout")
    public String layout() {
        return "layout-custom-area";
    }
}
