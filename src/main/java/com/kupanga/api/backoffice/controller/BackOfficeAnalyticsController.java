package com.kupanga.api.backoffice.controller;

import com.kupanga.api.backoffice.service.BienAdminService;
import com.kupanga.api.backoffice.service.DocumentAdminService;
import com.kupanga.api.backoffice.service.UserAdminService;
import com.kupanga.api.user.entity.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.security.Principal;

@Controller
@RequestMapping("/backoffice/analytics")
@RequiredArgsConstructor
public class BackOfficeAnalyticsController {

    private final UserAdminService     userAdminService;
    private final BienAdminService     bienAdminService;
    private final DocumentAdminService documentAdminService;

    @GetMapping
    public String analytics(Model model, Principal principal) {
        model.addAttribute("adminEmail",         principal.getName());
        model.addAttribute("totalUsers",         userAdminService.countAll());
        model.addAttribute("totalLocataires",    userAdminService.countByRole(Role.ROLE_LOCATAIRE));
        model.addAttribute("totalProprietaires", userAdminService.countByRole(Role.ROLE_PROPRIETAIRE));
        model.addAttribute("totalBiens",         bienAdminService.countAll());
        model.addAttribute("totalVilles",        bienAdminService.countDistinctVilles());
        model.addAttribute("biensParVille",      bienAdminService.getBiensParVille());
        model.addAttribute("biensParType",       bienAdminService.getBiensParType());
        model.addAttribute("totalContrats",      documentAdminService.countTotalContrats());
        model.addAttribute("totalEdl",           documentAdminService.countTotalEdl());
        model.addAttribute("totalQuittances",    documentAdminService.countTotalQuittances());
        model.addAttribute("documentsParBien",   documentAdminService.getDocumentsParBien());
        return "backoffice/analytics";
    }
}
