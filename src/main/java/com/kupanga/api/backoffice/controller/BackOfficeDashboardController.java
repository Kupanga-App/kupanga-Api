package com.kupanga.api.backoffice.controller;

import com.kupanga.api.backoffice.service.BienAdminService;
import com.kupanga.api.backoffice.service.UserAdminService;
import com.kupanga.api.user.entity.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.security.Principal;

@Controller
@RequestMapping("/backoffice/dashboard")
@RequiredArgsConstructor
public class BackOfficeDashboardController {

    private final UserAdminService userAdminService;
    private final BienAdminService bienAdminService;

    @GetMapping
    public String dashboard(Model model, Principal principal) {
        model.addAttribute("adminEmail",        principal.getName());
        model.addAttribute("totalUsers",        userAdminService.countAll());
        model.addAttribute("totalLocataires",   userAdminService.countByRole(Role.ROLE_LOCATAIRE));
        model.addAttribute("totalProprietaires",userAdminService.countByRole(Role.ROLE_PROPRIETAIRE));
        model.addAttribute("totalBiens",        bienAdminService.countAll());
        model.addAttribute("totalVilles",       bienAdminService.countDistinctVilles());
        model.addAttribute("biensParVille",     bienAdminService.getBiensParVille());
        model.addAttribute("biensParType",      bienAdminService.getBiensParType());
        return "backoffice/dashboard";
    }
}
