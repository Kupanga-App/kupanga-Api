package com.kupanga.api.immobilier.controller;

import com.kupanga.api.immobilier.dto.readDTO.DemandeLocationDTO;
import com.kupanga.api.immobilier.service.DemandeLocationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Affectation d'un locataire avec son accord : le propriétaire envoie une demande à un candidat, le bien n'est
 * assigné qu'à l'acceptation (remplace l'ancienne route {@code POST /biens/{id}/assigne-locataire/{userId}}).
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Demandes de location", description = "Proposer un bien à un candidat ; le bien est assigné quand il accepte")
public class DemandeLocationController {

    private final DemandeLocationService demandeLocationService;

    @Operation(summary = "Proposer le bien à un candidat",
            description = """
                    Envoie une demande de location à une personne qui a écrit au propriétaire au sujet de ce bien.
                    Le locataire reçoit une notification et un e-mail ; la demande est valable 72 heures.
                    Le bien n'est assigné qu'à l'acceptation.
                    """)
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Demande envoyée"),
            @ApiResponse(responseCode = "400", description = "Le compte visé n'est pas un compte locataire"),
            @ApiResponse(responseCode = "401", description = "Non authentifié"),
            @ApiResponse(responseCode = "403", description = "Pas propriétaire de ce bien"),
            @ApiResponse(responseCode = "404", description = "Bien introuvable, ou personne qui n'a pas écrit au sujet de ce bien"),
            @ApiResponse(responseCode = "409", description = "Bien archivé, déjà loué, ou demande déjà en attente")
    })
    @PreAuthorize("hasAuthority('ROLE_PROPRIETAIRE')")
    @PostMapping("/biens/{bienId}/demandes-location/{locataireId}")
    public ResponseEntity<DemandeLocationDTO> envoyer(
            @Parameter(description = "Identifiant du bien") @PathVariable Long bienId,
            @Parameter(description = "Identifiant du candidat") @PathVariable Long locataireId,
            Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(demandeLocationService.envoyer(auth.getName(), bienId, locataireId));
    }

    @Operation(summary = "Demandes envoyées pour un bien", description = "Historique, les plus récentes d'abord.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Liste des demandes"),
            @ApiResponse(responseCode = "403", description = "Pas propriétaire de ce bien"),
            @ApiResponse(responseCode = "404", description = "Bien introuvable")
    })
    @PreAuthorize("hasAuthority('ROLE_PROPRIETAIRE')")
    @GetMapping("/biens/{bienId}/demandes-location")
    public ResponseEntity<List<DemandeLocationDTO>> duBien(@PathVariable Long bienId, Authentication auth) {
        return ResponseEntity.ok(demandeLocationService.duBien(auth.getName(), bienId));
    }

    @Operation(summary = "Retirer une demande en attente")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Demande retirée"),
            @ApiResponse(responseCode = "403", description = "Demande d'un autre propriétaire"),
            @ApiResponse(responseCode = "404", description = "Demande introuvable"),
            @ApiResponse(responseCode = "409", description = "La demande n'est plus en attente")
    })
    @PreAuthorize("hasAuthority('ROLE_PROPRIETAIRE')")
    @DeleteMapping("/demandes-location/{id}")
    public ResponseEntity<Void> annuler(@PathVariable Long id, Authentication auth) {
        demandeLocationService.annuler(auth.getName(), id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Demandes reçues par le locataire connecté", description = "Demandes en attente et non expirées.")
    @PreAuthorize("hasAuthority('ROLE_LOCATAIRE')")
    @GetMapping("/demandes-location/recues")
    public ResponseEntity<List<DemandeLocationDTO>> recues(Authentication auth) {
        return ResponseEntity.ok(demandeLocationService.recues(auth.getName()));
    }

    @Operation(summary = "Accepter une demande", description = "Le bien est assigné au locataire connecté.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Demande acceptée, bien assigné"),
            @ApiResponse(responseCode = "404", description = "Demande introuvable ou adressée à un autre compte"),
            @ApiResponse(responseCode = "409", description = "Demande déjà traitée, bien archivé ou déjà loué"),
            @ApiResponse(responseCode = "410", description = "Demande expirée")
    })
    @PreAuthorize("hasAuthority('ROLE_LOCATAIRE')")
    @PostMapping("/demandes-location/{id}/accepter")
    public ResponseEntity<DemandeLocationDTO> accepter(@PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(demandeLocationService.accepter(auth.getName(), id));
    }

    @Operation(summary = "Refuser une demande")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Demande refusée"),
            @ApiResponse(responseCode = "404", description = "Demande introuvable ou adressée à un autre compte"),
            @ApiResponse(responseCode = "409", description = "Demande déjà traitée"),
            @ApiResponse(responseCode = "410", description = "Demande expirée")
    })
    @PreAuthorize("hasAuthority('ROLE_LOCATAIRE')")
    @PostMapping("/demandes-location/{id}/refuser")
    public ResponseEntity<DemandeLocationDTO> refuser(@PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(demandeLocationService.refuser(auth.getName(), id));
    }
}
