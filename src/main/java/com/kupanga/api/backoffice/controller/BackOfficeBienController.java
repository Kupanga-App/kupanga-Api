package com.kupanga.api.backoffice.controller;

import com.kupanga.api.juridiction.Pays;
import com.kupanga.api.backoffice.dto.BienAdminPageDTO;
import com.kupanga.api.backoffice.dto.BienAdminSearchDTO;
import com.kupanga.api.backoffice.service.BienAdminService;
import com.kupanga.api.immobilier.entity.TypeBien;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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
            @RequestParam(required = false)    Pays     pays,
            @RequestParam(required = false)    TypeBien typeBien,
            @RequestParam(defaultValue = "0")  int      page,
            @RequestParam(defaultValue = "10") int      size,
            Model model,
            Principal principal
    ) {
        BienAdminSearchDTO dto    = new BienAdminSearchDTO(titre, ville, pays, typeBien, page, size);
        BienAdminPageDTO   result = bienAdminService.rechercher(dto);

        model.addAttribute("adminEmail", principal.getName());
        model.addAttribute("page",       result);
        model.addAttribute("typesBien",  TypeBien.values());
        model.addAttribute("listePays",  Pays.values());
        model.addAttribute("titre",      titre);
        model.addAttribute("ville",      ville);
        model.addAttribute("pays",       pays);
        model.addAttribute("typeBien",   typeBien);
        model.addAttribute("size",       dto.size());
        return "backoffice/biens/list";
    }

    /** B12 : un bien n'est jamais supprimé, il est archivé (baux et quittances conservés). */
    @PostMapping("/{id}/archiver")
    public String archiver(@PathVariable Long id, RedirectAttributes redirect) {
        redirect.addFlashAttribute("message", bienAdminService.archiver(id)
                ? "Bien archivé : il n'est plus visible en ligne, ses documents sont conservés."
                : "Bien introuvable.");
        return "redirect:/backoffice/biens";
    }

    @PostMapping("/{id}/desarchiver")
    public String desarchiver(@PathVariable Long id, RedirectAttributes redirect) {
        redirect.addFlashAttribute("message", bienAdminService.desarchiver(id)
                ? "Bien remis en ligne."
                : "Impossible de remettre ce bien en ligne (introuvable ou propriétaire supprimé).");
        return "redirect:/backoffice/biens";
    }
}
