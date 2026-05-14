package com.kupanga.api.backoffice.controller;

import com.kupanga.api.backoffice.dto.BienAdminPageDTO;
import com.kupanga.api.backoffice.dto.BienAdminSearchDTO;
import com.kupanga.api.backoffice.service.BienAdminService;
import com.kupanga.api.immobilier.entity.TypeBien;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@Controller
@RequestMapping("/backoffice/biens")
@RequiredArgsConstructor
public class BackOfficeBienController {

    private final BienAdminService bienAdminService;

    @GetMapping
    public String list(
            @RequestParam(required = false)    String   titre,
            @RequestParam(required = false)    String   ville,
            @RequestParam(required = false)    TypeBien typeBien,
            @RequestParam(defaultValue = "0")  int      page,
            @RequestParam(defaultValue = "10") int      size,
            Model model,
            Principal principal
    ) {
        BienAdminSearchDTO dto    = new BienAdminSearchDTO(titre, ville, typeBien, page, size);
        BienAdminPageDTO   result = bienAdminService.rechercher(dto);

        model.addAttribute("adminEmail", principal.getName());
        model.addAttribute("page",       result);
        model.addAttribute("typesBien",  TypeBien.values());
        model.addAttribute("titre",      titre);
        model.addAttribute("ville",      ville);
        model.addAttribute("typeBien",   typeBien);
        model.addAttribute("size",       size);
        return "backoffice/biens/list";
    }

    @PostMapping("/{id}/supprimer")
    public String supprimer(@PathVariable Long id) {
        bienAdminService.supprimer(id);
        return "redirect:/backoffice/biens";
    }
}
