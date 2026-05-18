package com.kupanga.api.immobilier.controller;

import com.kupanga.api.immobilier.dto.formDTO.BienFormDTO;
import com.kupanga.api.immobilier.dto.formDTO.BienUpdateDTO;
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
import jakarta.validation.Valid;
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

    private final BienService       bienService;
    private final BienSearchService bienSearchService;

    // =========================================
    // CRÉER UN BIEN
    // =========================================
    @Operation(
            summary = "Créer un nouveau bien immobilier",
            description = """
                    Permet à un utilisateur authentifié ayant le rôle `PROPRIETAIRE` d'enregistrer
                    un nouveau bien immobilier. La localisation géographique est calculée automatiquement
                    à partir de l'adresse fournie via Nominatim (OpenStreetMap).
                    Les points d'intérêt (POI) sont calculés de façon asynchrone après la création.
                    Au moins une photo est obligatoire.

                    **Champs obligatoires :** titre, typeBien, adresse, ville, codePostal, pays,
                    surfaceHabitable, nombrePieces, loyerMensuel, chargesMensuelles, depotGarantie,
                    meuble, colocation, disponibleDe, images.

                    **Champs optionnels :** description, nombreChambres, etage, ascenseur,
                    anneeConstruction, modeChauffage, classeEnergie, classeGes.
                    """
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
                                            "titre":            "Le titre est obligatoire",
                                            "surfaceHabitable": "La surface habitable est obligatoire",
                                            "loyerMensuel":     "Le loyer mensuel est obligatoire",
                                            "disponibleDe":     "La date de disponibilité est obligatoire"
                                        },
                                        "timestamp": "2026-03-16T10:00:00"
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
                                    { "error": "Accès non autorisé" }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Accès refusé : l'utilisateur n'a pas le rôle PROPRIETAIRE",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    { "error": "Accès refusé : rôle PROPRIETAIRE requis" }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Géocodage échoué : adresse introuvable sur la carte",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    { "error": "Nous n'avons pas pu géolocaliser votre bien" }
                                    """)
                    )
            )
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Informations complètes du bien immobilier à créer",
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                            name = "Exemple complet",
                            value = """
                                    {
                                        "titre":              "Appartement lumineux 3 pièces",
                                        "typeBien":           "APPARTEMENT",
                                        "description":        "Bel appartement situé au centre-ville",
                                        "adresse":            "75 Boulevard Jules Verne",
                                        "ville":              "Nantes",
                                        "codePostal":         "44300",
                                        "pays":               "France",
                                        "surfaceHabitable":   65.5,
                                        "nombrePieces":       3,
                                        "nombreChambres":     2,
                                        "etage":              2,
                                        "ascenseur":          true,
                                        "anneeConstruction":  1998,
                                        "modeChauffage":      "ELECTRIQUE",
                                        "classeEnergie":      "C",
                                        "classeGes":          "B",
                                        "loyerMensuel":       850.00,
                                        "chargesMensuelles":  50.00,
                                        "depotGarantie":      1700.00,
                                        "meuble":             false,
                                        "colocation":         false,
                                        "disponibleDe":       "2026-04-01"
                                    }
                                    """
                    )
            )
    )
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> createBien(
            @Parameter(
                    description = "JSON contenant les informations du bien immobilier",
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
            description = """
                    Retourne les informations complètes d'un bien immobilier.
                    Seuls le propriétaire du bien et son locataire peuvent accéder à cette ressource.
                    Tout autre utilisateur, même authentifié, se verra refuser l'accès.

                    **Informations retournées :** caractéristiques physiques, conditions de location,
                    diagnostic énergétique, POI à proximité, documents, images, parties du contrat.
                    """
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
                                        "titre": "Appartement lumineux 3 pièces",
                                        "typeBien": "APPARTEMENT",
                                        "description": "Beau T3 lumineux avec vue dégagée",
                                        "adresse": "75 Boulevard Jules Verne",
                                        "ville": "Nantes",
                                        "codePostal": "44300",
                                        "pays": "France",
                                        "latitude": 47.2184,
                                        "longitude": -1.5536,
                                        "surfaceHabitable": 65.5,
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
                                        "disponibleDe": "2026-04-01",
                                        "proprietaire": {
                                            "firstName": "Jean",
                                            "lastName": "Dupont"
                                        },
                                        "locataire": null,
                                        "contrats":   ["https://minio.kupanga.com/contrats/contrat_1.pdf"],
                                        "quittances": [],
                                        "documents":  [],
                                        "images": [
                                            "https://minio.kupanga.com/biens/photo1.jpg",
                                            "https://minio.kupanga.com/biens/photo2.jpg"
                                        ],
                                        "pois": ["École", "Pharmacie", "Hôpital"],
                                        "createdAt": "2026-03-16T10:00:00",
                                        "updatedAt": "2026-03-16T10:00:00"
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
                                    { "error": "Accès non autorisé" }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Accès refusé : l'utilisateur n'est ni propriétaire ni locataire de ce bien",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    { "error": "Accès refusé au bien 1" }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Bien introuvable",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    { "error": "Bien introuvable : 1" }
                                    """)
                    )
            )
    })
    @GetMapping("/{bienId}")
    public ResponseEntity<BienDTO> getBienInfos(
            @Parameter(description = "Identifiant unique du bien immobilier", required = true)
            @PathVariable Long bienId
    ) {
        return ResponseEntity.ok(bienService.getBienInfos(bienId));
    }


    // =========================================
    // RECHERCHER DES BIENS
    // =========================================
    @Operation(
            summary = "Rechercher des biens immobiliers",
            description = """
                    Permet de rechercher des biens immobiliers avec des filtres dynamiques, un tri et une pagination.
                    Tous les champs sont optionnels — un body vide retourne tous les biens paginés.

                    **Filtres de localisation :**
                    Plusieurs villes, pays et codes postaux simultanément.

                    **Filtres de type :**
                    `APPARTEMENT`, `MAISON`, `STUDIO`, `VILLA`, `BUREAU`, `COMMERCE`.
                    Recherche partielle sur le titre (insensible à la casse).

                    **Filtres financiers :**
                    Loyer min/max (en €).

                    **Filtres de caractéristiques :**
                    Surface min/max (en m²), nombre de pièces minimum, étage min/max,
                    avec ou sans ascenseur, meublé ou non, colocation autorisée ou non,
                    disponible avant une date donnée.

                    **Filtres énergétiques :**
                    Classe énergie (`A` à `G`), classe GES (`A` à `G`).

                    **Filtres de chauffage :**
                    `ELECTRIQUE`, `GAZ`, `FIOUL`, `BOIS`, `POMPE_A_CHALEUR`, `POELE`, `COLLECTIF`, `SANS_CHAUFFAGE`.

                    **Filtres POI (points d'intérêt à moins de 5km) :**
                    `SCHOOL` — École, `HOSPITAL` — Hôpital, `PHARMACY` — Pharmacie, `KINDERGARTEN` — Garderie.
                    Plusieurs POI combinables — seuls les biens ayant **tous** les POI demandés sont retournés.
                    Données précalculées à la création — aucun appel externe lors de la recherche.

                    **Tri disponible :**
                    `id`, `titre`, `ville`, `codePostal`, `typeBien`, `loyerMensuel`, `surfaceHabitable`,
                    `disponibleDe`, `createdAt`. Défaut : `id ASC`.

                    **Pagination :**
                    Défaut : page `0`, taille `10`.
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
                                                "titre": "Appartement lumineux 3 pièces",
                                                "typeBien": "APPARTEMENT",
                                                "adresse": "75 Boulevard Jules Verne",
                                                "ville": "Nantes",
                                                "codePostal": "44300",
                                                "pays": "France",
                                                "latitude": 47.2184,
                                                "longitude": -1.5536,
                                                "surfaceHabitable": 65.5,
                                                "nombrePieces": 3,
                                                "etage": 2,
                                                "ascenseur": true,
                                                "modeChauffage": "ELECTRIQUE",
                                                "classeEnergie": "C",
                                                "classeGes": "B",
                                                "loyerMensuel": 850.00,
                                                "chargesMensuelles": 50.00,
                                                "meuble": false,
                                                "colocation": false,
                                                "disponibleDe": "2026-04-01",
                                                "images": [
                                                    "https://minio.kupanga.com/biens/photo1.jpg"
                                                ],
                                                "pois": ["École", "Pharmacie"]
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
                                            "sortDirection":  "Valeur acceptée : ASC ou DESC",
                                            "poisRequis":     "Valeurs acceptées : SCHOOL, HOSPITAL, PHARMACY, KINDERGARTEN",
                                            "classesEnergie": "Valeurs acceptées : A, B, C, D, E, F, G"
                                        },
                                        "timestamp": "2026-03-16T10:00:00"
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
                                    { "error": "Accès non autorisé" }
                                    """)
                    )
            )
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Critères de recherche, tri et pagination. Tous les champs sont optionnels.",
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    examples = {
                            @ExampleObject(
                                    name = "Recherche simple",
                                    value = """
                                            {
                                                "villes":        ["Nantes", "Angers"],
                                                "typesBien":     ["APPARTEMENT", "MAISON"],
                                                "loyerMax":      1000,
                                                "surfaceMin":    40,
                                                "page":          0,
                                                "size":          10,
                                                "sortBy":        "loyerMensuel",
                                                "sortDirection": "ASC"
                                            }
                                            """
                            ),
                            @ExampleObject(
                                    name = "Recherche avec tous les filtres",
                                    value = """
                                            {
                                                "villes":          ["Nantes"],
                                                "typesBien":       ["APPARTEMENT"],
                                                "loyerMin":        500,
                                                "loyerMax":        1000,
                                                "surfaceMin":      40,
                                                "surfaceMax":      100,
                                                "piecesMin":       2,
                                                "ascenseur":       true,
                                                "etageMin":        1,
                                                "etageMax":        5,
                                                "meuble":          false,
                                                "colocation":      false,
                                                "disponibleAvant": "2026-06-01",
                                                "classesEnergie":  ["A", "B", "C"],
                                                "classesGes":      ["A", "B"],
                                                "modesChauffage":  ["ELECTRIQUE", "GAZ"],
                                                "poisRequis":      ["SCHOOL", "PHARMACY"],
                                                "page":            0,
                                                "size":            10,
                                                "sortBy":          "loyerMensuel",
                                                "sortDirection":   "ASC"
                                            }
                                            """
                            ),
                            @ExampleObject(
                                    name = "Body vide — tous les biens",
                                    value = "{}"
                            )
                    }
            )
    )
    @PostMapping("/search")
    public ResponseEntity<BienPageDTO> rechercher(
            @RequestBody BienSearchDTO dto
    ) {
        return ResponseEntity.ok(bienSearchService.rechercher(dto));
    }

    // =========================================
    // MODIFIER UN BIEN (PATCH partiel)
    // =========================================
    @Operation(
            summary = "Modifier partiellement un bien immobilier",
            description = """
                    Permet au **propriétaire** du bien de mettre à jour un ou plusieurs champs.

                    ### Règles de mise à jour

                    **Convention `null` = inchangé :**
                    Tout champ absent du JSON (ou explicitement `null`) est ignoré —
                    la valeur existante en base est conservée.
                    Le front n'a donc pas à renvoyer l'intégralité des données du bien,
                    seulement ce qui change.

                    **Cas particulier — effacement de la description :**
                    `"description": null` → inchangé.
                    `"description": ""` → la description est effacée (mise à `null` en base).
                    C'est le seul champ nullable via cet endpoint.

                    ### Champs modifiables

                    | Champ               | Type       | Validation si fourni                        |
                    |---------------------|------------|---------------------------------------------|
                    | `titre`             | String     | 3–150 car., alphanum + `,.'"-()`, sans URL  |
                    | `typeBien`          | Enum       | `APPARTEMENT`, `MAISON`, `STUDIO`, `VILLA`, `BUREAU`, `COMMERCE` |
                    | `description`       | String     | max 1000 car., sans URL — `""` pour effacer |
                    | `surfaceHabitable`  | Double     | 9.0 – 10 000.0                              |
                    | `nombrePieces`      | Integer    | 1 – 50                                      |
                    | `nombreChambres`    | Integer    | 0 – 20                                      |
                    | `etage`             | Integer    | 0 – 200                                     |
                    | `ascenseur`         | Boolean    | —                                           |
                    | `anneeConstruction` | Integer    | 1800 – 2100                                 |
                    | `modeChauffage`     | Enum       | `ELECTRIQUE`, `GAZ`, `FIOUL`, `BOIS`, `POMPE_A_CHALEUR`, `POELE`, `COLLECTIF`, `SANS_CHAUFFAGE` |
                    | `classeEnergie`     | Enum       | `A` à `G`                                   |
                    | `classeGes`         | Enum       | `A` à `G`                                   |
                    | `loyerMensuel`      | Double     | 1.0 – 100 000.0, 2 décimales max            |
                    | `chargesMensuelles` | Double     | 0.0 – 10 000.0, 2 décimales max             |
                    | `depotGarantie`     | Double     | 0.0 – 100 000.0, 2 décimales max            |
                    | `meuble`            | Boolean    | —                                           |
                    | `colocation`        | Boolean    | —                                           |
                    | `disponibleDe`      | LocalDate  | format `YYYY-MM-DD`, aucune contrainte future/passé |

                    ### Champs non modifiables via cet endpoint
                    `adresse`, `ville`, `codePostal`, `pays`, `localisation` (géocodage géré séparément),
                    `proprietaire`, `locataire`, `contrats`, `quittances`, `images`, `documents`, `pois`.

                    ### Réponse
                    Retourne le bien complet mis à jour (`BienDTO`).
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Bien mis à jour avec succès — retourne le bien complet",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = BienDTO.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Données invalides — un champ fourni ne respecte pas sa contrainte de format",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                        "status": 400,
                                        "message": "Données invalides",
                                        "erreurs": {
                                            "titre":            "Entre 3 et 150 caractères",
                                            "loyerMensuel":     "Le loyer doit être supérieur à 0",
                                            "surfaceHabitable": "La surface minimale est de 9 m²"
                                        },
                                        "timestamp": "2026-05-18T10:00:00"
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
                                    { "error": "Accès non autorisé" }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Accès refusé — l'utilisateur n'est pas le propriétaire de ce bien",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    { "error": "Accès refusé : vous n'êtes pas le propriétaire de ce bien" }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Bien introuvable",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    { "error": "Aucun bien trouvé pour cet id" }
                                    """)
                    )
            )
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Champs à modifier. Seuls les champs présents et non-null sont appliqués.",
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    examples = {
                            @ExampleObject(
                                    name = "Modifier uniquement le loyer et la disponibilité",
                                    value = """
                                            {
                                                "loyerMensuel": 920.00,
                                                "disponibleDe": "2026-07-01"
                                            }
                                            """
                            ),
                            @ExampleObject(
                                    name = "Mise à jour complète des champs",
                                    value = """
                                            {
                                                "titre":             "Studio rénové centre-ville",
                                                "typeBien":          "STUDIO",
                                                "description":       "Entièrement rénové en 2025, calme et lumineux.",
                                                "surfaceHabitable":  28.5,
                                                "nombrePieces":      1,
                                                "nombreChambres":    0,
                                                "etage":             3,
                                                "ascenseur":         true,
                                                "anneeConstruction": 1975,
                                                "modeChauffage":     "ELECTRIQUE",
                                                "classeEnergie":     "D",
                                                "classeGes":         "D",
                                                "loyerMensuel":      650.00,
                                                "chargesMensuelles": 40.00,
                                                "depotGarantie":     1300.00,
                                                "meuble":            true,
                                                "colocation":        false,
                                                "disponibleDe":      "2026-06-15"
                                            }
                                            """
                            ),
                            @ExampleObject(
                                    name = "Effacer la description",
                                    value = """
                                            {
                                                "description": ""
                                            }
                                            """
                            )
                    }
            )
    )
    @PatchMapping("/{bienId}")
    public ResponseEntity<BienDTO> updateBien(
            @Parameter(description = "Identifiant unique du bien à modifier", required = true)
            @PathVariable Long bienId,
            @Valid @RequestBody BienUpdateDTO dto
    ) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return ResponseEntity.ok(bienService.updateBien(auth, bienId, dto));
    }


    @PostMapping("/{bienId}/assigne-locataire/{userId}")
    public ResponseEntity<Void> assignLocataire(@PathVariable Long bienId , @PathVariable Long userId){

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        bienService.affectLocataire(auth , bienId , userId);
        return ResponseEntity.noContent().build();
    }
}