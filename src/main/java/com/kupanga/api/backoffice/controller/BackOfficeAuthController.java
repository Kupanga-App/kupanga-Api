package com.kupanga.api.backoffice.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/backoffice")
public class BackOfficeAuthController {

    @GetMapping({"", "/"})
    public String root() {
        return "redirect:/backoffice/dashboard";
    }

    @GetMapping("/login")
    public String loginPage() {
        return "backoffice/login";
    }
}
