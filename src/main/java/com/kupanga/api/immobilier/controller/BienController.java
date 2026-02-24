package com.kupanga.api.immobilier.controller;

import com.kupanga.api.immobilier.dto.formDTO.BienFormDTO;
import com.kupanga.api.immobilier.dto.readDTO.BienResponseDTO;
import com.kupanga.api.immobilier.service.BienService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/biens")
@RequiredArgsConstructor
@Tag(name = "Gestion des Biens Immobiliers", description = "Endpoints pour gérer les biens immobiliers")
public class BienController {

    private final BienService bienService;

    @Operation(summary = "Créer un nouveau bien immobilier")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Bien créé avec succès"),
            @ApiResponse(responseCode = "400", description = "Données invalides"),
            @ApiResponse(responseCode = "404", description = "Propriétaire non trouvé")
    })
    @PostMapping
    public ResponseEntity<BienResponseDTO> createBien(@Valid @RequestBody BienFormDTO bienFormDTO) {
        BienResponseDTO createdBien = bienService.createBien(bienFormDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdBien);
    }

    @Operation(summary = "Récupérer tous les biens immobiliers")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Liste des biens récupérée")
    })
    @GetMapping
    public ResponseEntity<List<BienResponseDTO>> getAllBiens() {
        List<BienResponseDTO> biens = bienService.getAllBiens();
        return ResponseEntity.ok(biens);
    }

    @Operation(summary = "Récupérer un bien par son ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Bien trouvé"),
            @ApiResponse(responseCode = "404", description = "Bien non trouvé")
    })
    @GetMapping("/{id}")
    public ResponseEntity<BienResponseDTO> getBienById(@PathVariable Long id) {
        BienResponseDTO bien = bienService.getBienById(id);
        return ResponseEntity.ok(bien);
    }

    @Operation(summary = "Modifier un bien existant")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Bien modifié avec succès"),
            @ApiResponse(responseCode = "404", description = "Bien non trouvé"),
            @ApiResponse(responseCode = "400", description = "Données invalides")
    })
    @PutMapping("/{id}")
    public ResponseEntity<BienResponseDTO> updateBien(
            @PathVariable Long id,
            @Valid @RequestBody BienFormDTO bienFormDTO) {
        BienResponseDTO updatedBien = bienService.updateBien(id, bienFormDTO);
        return ResponseEntity.ok(updatedBien);
    }

    @Operation(summary = "Supprimer un bien")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Bien supprimé avec succès"),
            @ApiResponse(responseCode = "404", description = "Bien non trouvé")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBien(@PathVariable Long id) {
        bienService.deleteBien(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Récupérer les biens d'un propriétaire")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Liste des biens du propriétaire")
    })
    @GetMapping("/proprietaire/{proprietaireId}")
    public ResponseEntity<List<BienResponseDTO>> getBiensByProprietaire(@PathVariable Long proprietaireId) {
        List<BienResponseDTO> biens = bienService.getBiensByProprietaire(proprietaireId);
        return ResponseEntity.ok(biens);
    }

    @Operation(summary = "Récupérer les biens disponibles à la location")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Liste des biens disponibles")
    })
    @GetMapping("/disponibles")
    public ResponseEntity<List<BienResponseDTO>> getAvailableBiens() {
        List<BienResponseDTO> biens = bienService.getAvailableBiens();
        return ResponseEntity.ok(biens);
    }

    @Operation(summary = "Récupérer les biens d'une ville")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Liste des biens de la ville")
    })
    @GetMapping("/ville/{ville}")
    public ResponseEntity<List<BienResponseDTO>> getBiensByVille(@PathVariable String ville) {
        List<BienResponseDTO> biens = bienService.getBiensByVille(ville);
        return ResponseEntity.ok(biens);
    }
}
