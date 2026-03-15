package com.kupanga.api.immobilier.controller;

import com.kupanga.api.immobilier.dto.formDTO.ContratFormDTO;
import com.kupanga.api.immobilier.dto.formDTO.SignatureDTO;
import com.kupanga.api.immobilier.dto.readDTO.ContratDTO;
import com.kupanga.api.immobilier.service.ContratService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/contrats")
@RequiredArgsConstructor
public class ContratController {

    private final ContratService contratService;

    @PostMapping
    public ResponseEntity<Void> creerContrat(
            @Valid @RequestBody ContratFormDTO dto
    ) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        contratService.creerContrat(dto, auth.getName());

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/signer-proprio")
    public ResponseEntity<Void> signerProprietaire(
            @PathVariable Long id,
            @Valid @RequestBody SignatureDTO dto
    ) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        contratService.signerProprietaire(id, dto.signatureBase64(), auth.getName());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/signer/{token}")
    public ResponseEntity<ContratDTO> getContratParToken(
            @PathVariable String token
    ) {
        return ResponseEntity.ok(contratService.getContratParToken(token));
    }

    @PostMapping("/signer/{token}")
    public ResponseEntity<Void> signerLocataire(
            @PathVariable String token,
            @Valid @RequestBody SignatureDTO dto
    ) {
        contratService.signerLocataire(token, dto.signatureBase64());
        return ResponseEntity.noContent().build();
    }
}
