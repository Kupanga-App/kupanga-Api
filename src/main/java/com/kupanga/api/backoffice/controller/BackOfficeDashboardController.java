package com.kupanga.api.backoffice.controller;

import com.kupanga.api.backoffice.service.TableauDeBordService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.security.Principal;
import java.time.LocalDate;

@Controller
@RequestMapping("/backoffice/dashboard")
@RequiredArgsConstructor
public class BackOfficeDashboardController {

    private final TableauDeBordService tableauDeBordService;

    @GetMapping
    public String dashboard(Model model, Principal principal) {
        model.addAttribute("adminEmail", principal.getName());
        model.addAttribute("tdb", tableauDeBordService.calculer());
        model.addAttribute("aujourdhui", LocalDate.now());
        return "backoffice/dashboard";
    }
}
