package com.kupanga.api.backoffice.controller;

import com.kupanga.api.backoffice.service.BienAdminService;
import com.kupanga.api.backoffice.service.DocumentAdminService;
import com.kupanga.api.backoffice.service.EmailAdminService;
import com.kupanga.api.backoffice.service.UserAdminService;
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

    private final UserAdminService    userAdminService;
    private final BienAdminService    bienAdminService;
    private final DocumentAdminService documentAdminService;
    private final EmailAdminService   emailAdminService;

    @GetMapping
    public String dashboard(Model model, Principal principal) {
        model.addAttribute("adminEmail",         principal.getName());
        model.addAttribute("totalUsers",         userAdminService.countAll());
        model.addAttribute("totalLocataires",    userAdminService.countLocataires());
        model.addAttribute("totalProprietaires", userAdminService.countProprietaires());
        model.addAttribute("totalSansBienNiLogement", userAdminService.countSansBienNiLogement());
        model.addAttribute("totalBiens",         bienAdminService.countAll());
        model.addAttribute("totalVilles",        bienAdminService.countDistinctVilles());
        model.addAttribute("biensParVille",      bienAdminService.getBiensParVille());
        model.addAttribute("biensParType",       bienAdminService.getBiensParType());
        model.addAttribute("totalContrats",      documentAdminService.countTotalContrats());
        model.addAttribute("totalEdl",           documentAdminService.countTotalEdl());
        model.addAttribute("totalQuittances",    documentAdminService.countTotalQuittances());
        model.addAttribute("emailsEnEchec",      emailAdminService.compterEchecs());
        model.addAttribute("emailsEnAttente",    emailAdminService.compterEnAttente());
        return "backoffice/dashboard";
    }
}
