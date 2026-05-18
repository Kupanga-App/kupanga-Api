package com.kupanga.api.backoffice.controller;

import com.kupanga.api.backoffice.service.DocumentAdminService;
import com.kupanga.api.immobilier.entity.Bien;
import com.kupanga.api.immobilier.repository.BienRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;

@Controller
@RequestMapping("/backoffice/biens/{id}/documents")
@RequiredArgsConstructor
public class BackOfficeBienDocumentsController {

    private final DocumentAdminService documentAdminService;
    private final BienRepository       bienRepository;

    @GetMapping
    public String documents(@PathVariable Long id, Model model, Principal principal) {
        Bien bien = bienRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        model.addAttribute("adminEmail",  principal.getName());
        model.addAttribute("bienId",      id);
        model.addAttribute("bienTitre",   bien.getTitre());
        model.addAttribute("bienVille",   bien.getVille());
        model.addAttribute("contrats",    documentAdminService.getContratsParBien(id));
        model.addAttribute("edls",        documentAdminService.getEdlParBien(id));
        model.addAttribute("quittances",  documentAdminService.getQuittancesParBien(id));
        return "backoffice/biens/documents";
    }
}
