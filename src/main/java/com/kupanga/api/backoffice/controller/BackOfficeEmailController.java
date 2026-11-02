package com.kupanga.api.backoffice.controller;

import com.kupanga.api.backoffice.dto.EmailAdminPageDTO;
import com.kupanga.api.backoffice.dto.EmailAdminSearchDTO;
import com.kupanga.api.backoffice.service.EmailAdminService;
import com.kupanga.api.email.file.StatutEmail;
import com.kupanga.api.email.file.TypeEmail;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

/** M4 : page « E-mails » du back-office (file d'envoi, échecs, renvoi manuel). */
@Controller
@RequestMapping("/backoffice/emails")
@RequiredArgsConstructor
public class BackOfficeEmailController {

    private final EmailAdminService emailAdminService;

    @GetMapping
    public String list(
            @RequestParam(required = false)    StatutEmail statut,
            @RequestParam(required = false)    TypeEmail   type,
            @RequestParam(defaultValue = "0")  int         page,
            @RequestParam(defaultValue = "20") int         size,
            Model model,
            Principal principal
    ) {
        EmailAdminSearchDTO dto    = new EmailAdminSearchDTO(statut, type, page, size);
        EmailAdminPageDTO   result = emailAdminService.rechercher(dto);

        model.addAttribute("adminEmail", principal.getName());
        model.addAttribute("page",       result);
        model.addAttribute("statuts",    StatutEmail.values());
        model.addAttribute("types",      TypeEmail.values());
        model.addAttribute("statut",     statut);
        model.addAttribute("type",       type);
        model.addAttribute("size",       dto.size());
        model.addAttribute("enAttente",  emailAdminService.compterEnAttente());
        model.addAttribute("echecs",     emailAdminService.compterEchecs());
        return "backoffice/emails/list";
    }

    @PostMapping("/{id}/renvoyer")
    public String renvoyer(@PathVariable Long id, RedirectAttributes redirect) {
        String message = switch (emailAdminService.renvoyer(id)) {
            case REMIS_EN_FILE  -> "E-mail remis en file : il part dans les secondes qui suivent.";
            case INTROUVABLE    -> "E-mail introuvable.";
            case PAS_EN_ECHEC   -> "Seul un e-mail en échec peut être renvoyé.";
            case LIEN_EXPIRE    -> "Le lien de cet e-mail a expiré : la personne doit en redemander un.";
            case CONTENU_EFFACE -> "Le contenu de cet e-mail a été effacé (conservation de 30 jours) : renvoi impossible.";
        };
        redirect.addFlashAttribute("message", message);
        return "redirect:/backoffice/emails";
    }
}
