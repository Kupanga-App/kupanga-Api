package com.kupanga.api.immobilier.controller;

import com.kupanga.api.immobilier.dto.readDTO.LocataireDashboardDTO;
import com.kupanga.api.immobilier.service.LocataireDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/locataire")
@RequiredArgsConstructor
@Tag(name = "Dashboard Locataire", description = "Vue agrégée du bail : bien, contrat, quittances et états des lieux")
public class LocataireDashboardController {

    private final LocataireDashboardService dashboardService;

    @Operation(
            summary = "Dashboard locataire d'un bien",
            description = """
                    Retourne en une seule réponse toutes les informations du bail actif :
                    - résumé du locataire (prénom, nom, initiales)
                    - résumé du bien (adresse, surface, type, photos…)
                    - contrat signé actif (loyer, durée, mois écoulés, statut)
                    - statuts rapides (dernière quittance, date de fin de bail)
                    - liste complète des quittances
                    - liste complète des états des lieux

                    L'utilisateur authentifié doit être le locataire du bien demandé,
                    sans quoi une erreur **403 Forbidden** est retournée.
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Dashboard retourné avec succès",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = LocataireDashboardDTO.class)
                    )
            ),
            @ApiResponse(responseCode = "401", description = "Non authentifié"),
            @ApiResponse(responseCode = "403", description = "L'utilisateur n'est pas le locataire de ce bien"),
            @ApiResponse(responseCode = "404", description = "Bien introuvable")
    })
    @GetMapping("/dashboard/{bienId}")
    public ResponseEntity<LocataireDashboardDTO> getDashboard(
            @Parameter(description = "Identifiant du bien", required = true)
            @PathVariable Long bienId
    ) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return ResponseEntity.ok(dashboardService.getDashboard(auth, bienId));
    }
}
