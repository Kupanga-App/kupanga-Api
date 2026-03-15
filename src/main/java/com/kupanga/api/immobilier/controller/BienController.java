package com.kupanga.api.immobilier.controller;

import com.kupanga.api.immobilier.dto.formDTO.BienFormDTO;
import com.kupanga.api.immobilier.dto.readDTO.BienDTO;
import com.kupanga.api.immobilier.research.BienSearchService;
import com.kupanga.api.immobilier.research.dto.BienPageDTO;
import com.kupanga.api.immobilier.research.dto.BienSearchDTO;
import com.kupanga.api.immobilier.service.BienService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "Biens", description = "Gestion des biens immobiliers")
@RestController
@RequestMapping("/biens")
@RequiredArgsConstructor
public class BienController {

    private final BienService bienService;
    private final BienSearchService bienSearchService;

    // =========================================
    // CRÉER UN BIEN
    // =========================================
    @Operation(
            summary = "Créer un nouveau bien immobilier",
            description = "Permet à un utilisateur authentifié ayant le rôle PROPRIETAIRE d'enregistrer " +
                    "un nouveau bien immobilier. La localisation géographique est calculée automatiquement " +
                    "à partir de l'adresse fournie. Au moins une photo est obligatoire."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "204",
                    description = "Bien créé avec succès"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Requête invalide : champs manquants, incorrects ou aucune image fournie",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                {
                                    "status": 400,
                                    "message": "Données invalides",
                                    "erreurs": {
                                        "titre": "Le titre est obligatoire",
                                        "ville": "Lettres, espaces, tirets ou apostrophes uniquement",
                                        "codePostal": "Code postal invalide"
                                    },
                                    "timestamp": "2026-03-15T10:00:00"
                                }
                                """)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Utilisateur non authentifié ou token invalide",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                {
                                    "error": "Accès non autorisé"
                                }
                                """)
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Accès refusé : l'utilisateur n'a pas le rôle PROPRIETAIRE",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                {
                                    "error": "Accès refusé : rôle PROPRIETAIRE requis"
                                }
                                """)
                    )
            ),
            @ApiResponse(
                    responseCode = "422",
                    description = "Géocodage échoué : adresse introuvable sur la carte",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                {
                                    "error": "Impossible de localiser l'adresse fournie"
                                }
                                """)
                    )
            )
    })
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> createBien(
            @Parameter(
                    description = "JSON contenant les informations obligatoires du bien immobilier : " +
                            "titre, typeBien, adresse, ville, codePostal, pays et optionnellement description.",
                    required = true
            )
            @RequestPart("bienFormDTO") BienFormDTO bienFormDTO,

            @Parameter(
                    description = "Photos du bien (JPG, PNG, WEBP — 10 Mo max par fichier). " +
                            "Au moins une image est obligatoire. " +
                            "Envoyer plusieurs fichiers en répétant la clé 'files'.",
                    required = true
            )
            @RequestPart(value = "files", required = false) List<MultipartFile> files
    ) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        bienService.createBien(auth, bienFormDTO, files);
        return ResponseEntity.noContent().build();
    }


    // =========================================
    // CONSULTER UN BIEN
    // =========================================
    @Operation(
            summary = "Consulter le détail d'un bien immobilier",
            description = "Retourne les informations complètes d'un bien immobilier. " +
                    "Seuls le propriétaire du bien et son locataire peuvent accéder à cette ressource. " +
                    "Tout autre utilisateur, même authentifié, se verra refuser l'accès."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Bien trouvé et retourné avec succès",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = BienDTO.class),
                            examples = @ExampleObject(value = """
                                {
                                    "id": 1,
                                    "titre": "Appartement T3 - Nantes Centre",
                                    "typeBien": "APPARTEMENT",
                                    "description": "Beau T3 lumineux avec vue dégagée",
                                    "adresse": "12 rue de la Paix",
                                    "ville": "Nantes",
                                    "codePostal": "44000",
                                    "pays": "France",
                                    "latitude": 47.2184,
                                    "longitude": -1.5536,
                                    "createdAt": "2026-03-15T10:00:00",
                                    "updatedAt": "2026-03-15T10:00:00",
                                    "proprietaire": {
                                        "id": 5,
                                        "nom": "Dupont",
                                        "prenom": "Jean",
                                        "email": "jean.dupont@email.com"
                                    },
                                    "locataire": null,
                                    "contrats": [],
                                    "quittances": [],
                                    "documents": [],
                                    "images": [
                                        "https://minio.kupanga.com/biens/photo1.jpg",
                                        "https://minio.kupanga.com/biens/photo2.jpg"
                                    ]
                                }
                                """)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Utilisateur non authentifié ou token invalide",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                {
                                    "error": "Accès non autorisé"
                                }
                                """)
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Accès refusé : l'utilisateur n'est ni propriétaire ni locataire de ce bien",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                {
                                    "error": "Accès refusé au bien 1"
                                }
                                """)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Bien introuvable",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                {
                                    "error": "Bien introuvable : 1"
                                }
                                """)
                    )
            )
    })
    @GetMapping("/{bienId}")
    public ResponseEntity<BienDTO> getBienInfos(
            @Parameter(description = "Identifiant unique du bien immobilier", required = true)
            @PathVariable Long bienId
    ) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return ResponseEntity.ok(bienService.getBienInfos(auth, bienId));
    }

    @Operation(
            summary = "Rechercher des biens immobiliers",
            description = """
                Permet de rechercher des biens immobiliers avec des filtres dynamiques, un tri et une pagination.
                
                **Filtres disponibles :**
                - Plusieurs villes simultanément
                - Plusieurs pays simultanément
                - Plusieurs codes postaux simultanément
                - Plusieurs types de bien simultanément (`APPARTEMENT`, `MAISON`, `STUDIO`, `VILLA`, `BUREAU`, `COMMERCE`)
                - Recherche partielle sur le titre (insensible à la casse)
                
                **Tri disponible :**
                Les champs de tri acceptés sont : `id`, `titre`, `ville`, `codePostal`, `typeBien`, `createdAt`.
                Par défaut le tri est effectué par `id` en ordre croissant (`ASC`).
                
                **Pagination :**
                Par défaut la page est `0` et la taille est `10`.
                Tous les champs sont optionnels — un body vide retourne tous les biens paginés.
                """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Résultats de recherche retournés avec succès",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = BienPageDTO.class),
                            examples = @ExampleObject(value = """
                                {
                                    "contenu": [
                                        {
                                            "id": 1,
                                            "titre": "Appartement lumineux T3",
                                            "typeBien": "APPARTEMENT",
                                            "adresse": "12 rue de la Paix",
                                            "ville": "Nantes",
                                            "codePostal": "44000",
                                            "pays": "France",
                                            "latitude": 47.2184,
                                            "longitude": -1.5536,
                                            "images": [
                                                "https://minio.kupanga.com/biens/photo1.jpg"
                                            ]
                                        }
                                    ],
                                    "pageActuelle": 0,
                                    "totalPages": 3,
                                    "totalElements": 25,
                                    "dernierePage": false,
                                    "premierePage": true
                                }
                                """)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Corps de la requête invalide",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                {
                                    "status": 400,
                                    "message": "Données invalides",
                                    "erreurs": {
                                        "sortDirection": "Valeur acceptée : ASC ou DESC"
                                    },
                                    "timestamp": "2026-03-15T10:00:00"
                                }
                                """)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Utilisateur non authentifié ou token invalide",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                {
                                    "error": "Accès non autorisé"
                                }
                                """)
                    )
            )
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Critères de recherche, tri et pagination. Tous les champs sont optionnels.",
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                            name = "Exemple de recherche",
                            value = """
                                {
                                    "villes":        ["Nantes", "Angers"],
                                    "pays":          ["France"],
                                    "codesPostaux":  ["44000", "49100"],
                                    "typesBien":     ["APPARTEMENT", "MAISON"],
                                    "titre":         "lumineux",
                                    "page":          0,
                                    "size":          10,
                                    "sortBy":        "ville",
                                    "sortDirection": "ASC"
                                }
                                """
                    )
            )
    )
    @PostMapping("/search")
    public ResponseEntity<BienPageDTO> rechercher(
            @RequestBody BienSearchDTO dto
    ) {
        return ResponseEntity.ok(bienSearchService.rechercher(dto));
    }
}