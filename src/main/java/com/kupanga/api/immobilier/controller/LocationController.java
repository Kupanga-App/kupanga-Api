package com.kupanga.api.immobilier.controller;

import com.kupanga.api.immobilier.dto.readDTO.LocationDTO;
import com.kupanga.api.immobilier.service.LocationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Réservations et locations (décision du 2026-11-05) : un bien accepté par un candidat lui est réservé
 * 14 jours ; il devient locataire à la signature du bail. Pas de contrôle par rôle (lot 3 : droits par relation) :
 * chaque route vérifie que le compte est le propriétaire du bien ou le candidat de la réservation.
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Locations", description = "Réservation d'un bien jusqu'à la signature du bail, désistement, retrait")
public class LocationController {

    private final LocationService locationService;

    @Operation(summary = "Réservation ou location en cours d'un bien",
            description = "Réservation encore valable, sinon location active ; 204 si le bien est libre.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Location en cours"),
            @ApiResponse(responseCode = "204", description = "Bien libre"),
            @ApiResponse(responseCode = "401", description = "Non authentifié"),
            @ApiResponse(responseCode = "403", description = "Pas propriétaire de ce bien"),
            @ApiResponse(responseCode = "404", description = "Bien introuvable")
    })
    @GetMapping("/biens/{bienId}/location")
    public ResponseEntity<LocationDTO> enCoursDuBien(
            @Parameter(description = "Identifiant du bien") @PathVariable Long bienId, Authentication auth) {
        return locationService.enCoursDuBien(auth.getName(), bienId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @Operation(summary = "Mes réservations",
            description = "Logements réservés par le compte connecté, en attente de la signature du bail.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Réservations en cours"),
            @ApiResponse(responseCode = "401", description = "Non authentifié")
    })
    @GetMapping("/locations/reservations")
    public ResponseEntity<List<LocationDTO>> mesReservations(Authentication auth) {
        return ResponseEntity.ok(locationService.mesReservations(auth.getName()));
    }

    @Operation(summary = "Se désister",
            description = """
                    Le candidat renonce au logement avant d'avoir signé le bail. Le bail non signé est annulé,
                    le propriétaire est prévenu et l'annonce revient dans la recherche.
                    """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Désistement enregistré"),
            @ApiResponse(responseCode = "401", description = "Non authentifié"),
            @ApiResponse(responseCode = "404", description = "Réservation introuvable"),
            @ApiResponse(responseCode = "409", description = "Bail déjà signé, réservation expirée ou terminée")
    })
    @PostMapping("/locations/{id}/desister")
    public ResponseEntity<LocationDTO> desister(@PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(locationService.desister(auth.getName(), id));
    }

    @Operation(summary = "Donner congé",
            description = """
                    Le propriétaire ou le locataire de la location en cours annonce la date de fin du bail
                    (aujourd'hui au plus tôt, dans les deux ans). L'autre partie est prévenue (notification et e-mail).
                    Aucun préavis légal n'est imposé par l'application.
                    """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Congé enregistré"),
            @ApiResponse(responseCode = "400", description = "Date absente ou hors bornes"),
            @ApiResponse(responseCode = "404", description = "Location introuvable ou d'un autre compte"),
            @ApiResponse(responseCode = "409", description = "Location pas en cours, ou congé déjà donné")
    })
    @PostMapping("/locations/{id}/conge")
    public ResponseEntity<LocationDTO> donnerConge(@PathVariable Long id,
                                                   @jakarta.validation.Valid @RequestBody
                                                   com.kupanga.api.immobilier.dto.formDTO.CongeDTO dto,
                                                   Authentication auth) {
        return ResponseEntity.ok(locationService.donnerConge(auth.getName(), id, dto.dateFin()));
    }

    @Operation(summary = "Retirer son congé", description = "Seule la personne qui a donné congé peut le retirer.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Congé retiré, le bail continue"),
            @ApiResponse(responseCode = "404", description = "Location introuvable ou d'un autre compte"),
            @ApiResponse(responseCode = "409", description = "Aucun congé, ou congé donné par l'autre partie")
    })
    @DeleteMapping("/locations/{id}/conge")
    public ResponseEntity<LocationDTO> retirerConge(@PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(locationService.retirerConge(auth.getName(), id));
    }

    @Operation(summary = "Enregistrer la fin du bail",
            description = """
                    Le propriétaire enregistre la fin du bail, à partir de la date annoncée par le congé, avec le
                    dépôt de garantie restitué (motif obligatoire en cas de retenue). Le bien est libéré et revient
                    dans les annonces ; le locataire garde ses documents.
                    """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Location terminée"),
            @ApiResponse(responseCode = "400", description = "Montant supérieur au dépôt, ou motif de retenue manquant"),
            @ApiResponse(responseCode = "404", description = "Location introuvable ou d'un autre propriétaire"),
            @ApiResponse(responseCode = "409", description = "Pas de congé, date pas encore atteinte, ou location pas en cours")
    })
    @PostMapping("/locations/{id}/terminer")
    public ResponseEntity<LocationDTO> terminer(@PathVariable Long id,
                                                @jakarta.validation.Valid @RequestBody
                                                com.kupanga.api.immobilier.dto.formDTO.FinDeBailDTO dto,
                                                Authentication auth) {
        return ResponseEntity.ok(locationService.terminer(auth.getName(), id, dto.depotRestitue(), dto.motifRetenue()));
    }

    @Operation(summary = "Retirer la réservation",
            description = """
                    Le propriétaire met fin à la réservation avant la signature du bail. Le bail non signé est
                    annulé et le candidat est prévenu ; le bien peut être proposé à une autre personne.
                    """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Réservation retirée"),
            @ApiResponse(responseCode = "401", description = "Non authentifié"),
            @ApiResponse(responseCode = "404", description = "Réservation introuvable ou d'un autre propriétaire"),
            @ApiResponse(responseCode = "409", description = "Bail déjà signé, réservation expirée ou terminée")
    })
    @PostMapping("/locations/{id}/annuler")
    public ResponseEntity<LocationDTO> annuler(@PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(locationService.annuler(auth.getName(), id));
    }
}
