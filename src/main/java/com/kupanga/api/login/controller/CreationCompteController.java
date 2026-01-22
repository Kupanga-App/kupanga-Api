package com.kupanga.api.login.controller;

import com.kupanga.api.exception.business.UserAlreadyExistsException;
import com.kupanga.api.login.service.CreationCompteService;
import com.kupanga.api.utilisateur.dto.formDTO.UtilisateurFormDTO;
import com.kupanga.api.utilisateur.dto.readDTO.UtilisateurDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/public/creationCompte")
public class CreationCompteController {

    private final CreationCompteService creationCompteService;

    @PostMapping
    public ResponseEntity<UtilisateurDTO> creationCompte(@RequestBody UtilisateurFormDTO utilisateurFormDTO) throws UserAlreadyExistsException {

        return ResponseEntity.ok(creationCompteService
                .creationUtilisateur(utilisateurFormDTO.email(), utilisateurFormDTO.role()));
    }
}
