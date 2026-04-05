package com.kupanga.api.user.controller;

import com.kupanga.api.immobilier.dto.readDTO.BienDTO;
import com.kupanga.api.immobilier.service.BienService;
import com.kupanga.api.user.research.LocataireSearchService;
import com.kupanga.api.user.research.dto.LocatairePageDTO;
import com.kupanga.api.user.research.dto.LocataireSearchDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Tag(name = "Utilisateurs", description = "Opérations liées au profil et aux biens de l'utilisateur connecté")
public class UserController {

    private final BienService bienService;
    private final LocataireSearchService locataireSearchService;

    // =========================================
    // MES BIENS
    // =========================================
    @Operation(
            summary = "Lister mes biens",
            description = """
                    Retourne la liste complète des biens associés au propriétaire connecté.
                    Chaque bien inclut ses informations générales, ses caractéristiques physiques,
                    son diagnostic énergétique, ses conditions de location, le locataire actuel
                    ainsi que les références aux contrats, quittances, documents, images et POIs.
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Liste des biens retournée avec succès",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = BienDTO.class),
                            examples = @ExampleObject(value = """
                                    [
                                      {
                                        "id": 1,
                                        "titre": "Appartement T3 centre-ville",
                                        "typeBien": "APPARTEMENT",
                                        "description": "Bel appartement lumineux avec vue dégagée",
                                        "adresse": "75 Boulevard Jules Verne",
                                        "ville": "Nantes",
                                        "codePostal": "44000",
                                        "pays": "France",
                                        "latitude": 47.2184,
                                        "longitude": -1.5536,
                                        "surfaceHabitable": 68.5,
                                        "nombrePieces": 3,
                                        "nombreChambres": 2,
                                        "etage": 2,
                                        "ascenseur": true,
                                        "anneeConstruction": 1998,
                                        "modeChauffage": "ELECTRIQUE",
                                        "classeEnergie": "C",
                                        "classeGes": "B",
                                        "loyerMensuel": 850.00,
                                        "chargesMensuelles": 50.00,
                                        "depotGarantie": 1700.00,
                                        "meuble": false,
                                        "colocation": false,
                                        "disponibleDe": null,
                                        "proprietaire": {
                                          "id": 1,
                                          "firstName": "Jean",
                                          "lastName": "Dupont",
                                          "mail": "jean.dupont@email.com"
                                        },
                                        "locataire": {
                                          "id": 2,
                                          "firstName": "Moise",
                                          "lastName": "Aganze",
                                          "mail": "moiseaganze76@gmail.com"
                                        },
                                        "contrats": [
                                          "https://minio.kupanga.com/contrats/contrats_numero_1.pdf"
                                        ],
                                        "quittances": [
                                          "https://minio.kupanga.com/quittances/quittance_1_2026_03.pdf"
                                        ],
                                        "documents": [],
                                        "images": [
                                          "https://minio.kupanga.com/images/bien_1_facade.jpg"
                                        ],
                                        "pois": ["Métro ligne 1", "École primaire"],
                                        "createdAt": "2026-01-15T10:30:00",
                                        "updatedAt": "2026-03-19T12:00:00"
                                      }
                                    ]
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "200",
                    description = "Liste vide si le propriétaire n'a aucun bien",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = "[]")
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Utilisateur non authentifié",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    { "error": "Accès non autorisé" }
                                    """)
                    )
            )
    })
    @GetMapping("/biens")
    ResponseEntity<List<BienDTO>> getAllPropertiesAssociateToOwner() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return ResponseEntity.ok(bienService.findAllPropertiesAssociateToUser(auth.getName()));
    }

    @PostMapping("/{bienId}/recherche-locataire")
    public ResponseEntity<LocatairePageDTO> rechercherLocataires(
            @PathVariable Long bienId,
            @RequestBody(required = false) LocataireSearchDTO dto
    ) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();


        if (dto == null) dto = new LocataireSearchDTO(
                null, null, null, null, null, null, null
        );
        return ResponseEntity.ok(locataireSearchService.rechercher( auth.getName() ,bienId, dto));
    }
}