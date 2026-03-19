package com.kupanga.api.immobilier.controller;

import com.kupanga.api.immobilier.dto.formDTO.EtatDesLieuxFormDTO;
import com.kupanga.api.immobilier.dto.formDTO.SignatureDTO;
import com.kupanga.api.immobilier.dto.readDTO.EtatDesLieuxDTO;
import com.kupanga.api.immobilier.service.EtatDesLieuxService;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/etats-des-lieux")
@RequiredArgsConstructor
@Tag(name = "États des lieux", description = "Gestion des états des lieux d'entrée et de sortie avec signatures électroniques")
public class EtatDesLieuxController {

    private final EtatDesLieuxService edlService;

    // =========================================
    // CRÉER UN ÉTAT DES LIEUX
    // =========================================
    @Operation(
            summary = "Créer un état des lieux",
            description = """
                    Permet à un propriétaire authentifié de créer un état des lieux
                    (entrée ou sortie) pour un de ses biens.
                    Le PDF est généré automatiquement et stocké sur MinIO.
                    L'EDL passe au statut `EN_ATTENTE_SIGNATURE_PROPRIO` après création.
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "État des lieux créé avec succès"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Données invalides",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                        "status": 400,
                                        "message": "Données invalides",
                                        "erreurs": {
                                            "bienId": "L'identifiant du bien est obligatoire",
                                            "dateRealisation": "La date de réalisation est obligatoire",
                                            "type": "Le type (ENTREE / SORTIE) est obligatoire"
                                        },
                                        "timestamp": "2026-03-19T10:00:00"
                                    }
                                    """)
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
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Bien ou locataire introuvable",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    { "error": "Bien introuvable : 1" }
                                    """)
                    )
            )
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Données complètes de l'état des lieux (pièces, éléments, compteurs, clés)",
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    examples = {
                            @ExampleObject(
                                    name = "EDL Entrée",
                                    summary = "Exemple d'état des lieux d'entrée",
                                    value = """
                                            {
                                              "bienId": 1,
                                              "emailLocataire": "test@gmail.com",
                                              "type": "ENTREE",
                                              "dateRealisation": "2026-03-19",
                                              "heureRealisation": "09:30",
                                              "observations": "État général satisfaisant. Logement propre à la remise des clés.",
                                              "compteurs": [
                                                { "typeCompteur": "ELECTRICITE_HP", "numeroCompteur": "PDL-123456789", "index": 4521.0, "unite": "kWh" },
                                                { "typeCompteur": "ELECTRICITE_HC", "numeroCompteur": "PDL-123456789", "index": 2103.5, "unite": "kWh" },
                                                { "typeCompteur": "EAU_FROIDE",     "numeroCompteur": "CPT-EAU-001",   "index": 187.3,  "unite": "m³"  }
                                              ],
                                              "cles": [
                                                { "typeCle": "Porte d'entrée",    "quantite": 2 },
                                                { "typeCle": "Boîte aux lettres", "quantite": 1 },
                                                { "typeCle": "Interphone",        "quantite": 1 }
                                              ],
                                              "pieces": [
                                                {
                                                  "nomPiece": "Entrée", "ordre": 1, "observations": null,
                                                  "elements": [
                                                    { "typeElement": "SOL",      "etatElement": "BON", "description": "Carrelage beige",  "observation": null },
                                                    { "typeElement": "MUR",      "etatElement": "BON", "description": "Peinture blanche", "observation": null },
                                                    { "typeElement": "PLAFOND",  "etatElement": "BON", "description": "Peinture blanche", "observation": null },
                                                    { "typeElement": "PORTE",    "etatElement": "BON", "description": "Porte blindée",    "observation": null },
                                                    { "typeElement": "LUMINAIRE","etatElement": "BON", "description": "Plafonnier LED",   "observation": null }
                                                  ]
                                                },
                                                {
                                                  "nomPiece": "Salon", "ordre": 2, "observations": "Légère marque sur le mur côté fenêtre.",
                                                  "elements": [
                                                    { "typeElement": "SOL",      "etatElement": "BON",          "description": "Parquet chêne",               "observation": null },
                                                    { "typeElement": "MUR",      "etatElement": "USAGE_NORMAL", "description": "Peinture blanche",            "observation": "Légère marque ~5 cm côté fenêtre" },
                                                    { "typeElement": "PLAFOND",  "etatElement": "BON",          "description": "Peinture blanche",            "observation": null },
                                                    { "typeElement": "FENETRE",  "etatElement": "BON",          "description": "Double vitrage PVC",          "observation": null },
                                                    { "typeElement": "VOLET",    "etatElement": "BON",          "description": "Volet roulant électrique",    "observation": null },
                                                    { "typeElement": "RADIATEUR","etatElement": "BON",          "description": "Radiateur électrique inertie","observation": null },
                                                    { "typeElement": "PRISE",    "etatElement": "BON",          "description": "4 prises + 1 TV",             "observation": null },
                                                    { "typeElement": "LUMINAIRE","etatElement": "BON",          "description": "Plafonnier + applique",       "observation": null }
                                                  ]
                                                },
                                                {
                                                  "nomPiece": "Cuisine", "ordre": 3, "observations": null,
                                                  "elements": [
                                                    { "typeElement": "SOL",       "etatElement": "BON", "description": "Carrelage blanc",         "observation": null },
                                                    { "typeElement": "MUR",       "etatElement": "BON", "description": "Faïence + peinture",      "observation": null },
                                                    { "typeElement": "PLAFOND",   "etatElement": "BON", "description": "Peinture blanche",        "observation": null },
                                                    { "typeElement": "EQUIPEMENT","etatElement": "BON", "description": "Plaque induction 4 feux", "observation": null },
                                                    { "typeElement": "EQUIPEMENT","etatElement": "BON", "description": "Four encastrable",        "observation": null },
                                                    { "typeElement": "EQUIPEMENT","etatElement": "USAGE_NORMAL", "description": "Hotte aspirante", "observation": "Filtre à changer" },
                                                    { "typeElement": "EQUIPEMENT","etatElement": "BON", "description": "Réfrigérateur combiné",   "observation": null },
                                                    { "typeElement": "LUMINAIRE", "etatElement": "BON", "description": "Spots encastrés LED",     "observation": null }
                                                  ]
                                                },
                                                {
                                                  "nomPiece": "WC", "ordre": 4, "observations": null,
                                                  "elements": [
                                                    { "typeElement": "SOL",       "etatElement": "BON",     "description": "Carrelage gris",   "observation": null },
                                                    { "typeElement": "MUR",       "etatElement": "BON",     "description": "Peinture blanche", "observation": null },
                                                    { "typeElement": "EQUIPEMENT","etatElement": "MAUVAIS", "description": "WC suspendu",      "observation": "Chasse d'eau à réparer — fuite constatée" },
                                                    { "typeElement": "LUMINAIRE", "etatElement": "BON",     "description": "Plafonnier",       "observation": null }
                                                  ]
                                                }
                                              ]
                                            }
                                            """
                            ),
                            @ExampleObject(
                                    name = "EDL Sortie",
                                    summary = "Exemple d'état des lieux de sortie",
                                    value = """
                                            {
                                              "bienId": 1,
                                              "emailLocataire": "test@gmail.com",
                                              "type": "SORTIE",
                                              "dateRealisation": "2026-09-19",
                                              "heureRealisation": "10:00",
                                              "observations": "État général correct. Quelques dégradations constatées par rapport à l'état des lieux d'entrée.",
                                              "compteurs": [
                                                { "typeCompteur": "ELECTRICITE_HP", "numeroCompteur": "PDL-123456789", "index": 5847.0, "unite": "kWh" },
                                                { "typeCompteur": "ELECTRICITE_HC", "numeroCompteur": "PDL-123456789", "index": 2891.5, "unite": "kWh" },
                                                { "typeCompteur": "EAU_FROIDE",     "numeroCompteur": "CPT-EAU-001",   "index": 241.7,  "unite": "m³"  }
                                              ],
                                              "cles": [
                                                { "typeCle": "Porte d'entrée",    "quantite": 2 },
                                                { "typeCle": "Boîte aux lettres", "quantite": 1 },
                                                { "typeCle": "Interphone",        "quantite": 1 }
                                              ],
                                              "pieces": [
                                                {
                                                  "nomPiece": "Entrée", "ordre": 1, "observations": null,
                                                  "elements": [
                                                    { "typeElement": "SOL",      "etatElement": "BON",          "description": "Carrelage beige",  "observation": null },
                                                    { "typeElement": "MUR",      "etatElement": "BON",          "description": "Peinture blanche", "observation": null },
                                                    { "typeElement": "PLAFOND",  "etatElement": "BON",          "description": "Peinture blanche", "observation": null },
                                                    { "typeElement": "PORTE",    "etatElement": "USAGE_NORMAL", "description": "Porte blindée",    "observation": "Légères rayures sur la poignée" },
                                                    { "typeElement": "LUMINAIRE","etatElement": "BON",          "description": "Plafonnier LED",   "observation": null }
                                                  ]
                                                },
                                                {
                                                  "nomPiece": "Salon", "ordre": 2, "observations": "Trou dans le mur côté TV, non présent à l'entrée.",
                                                  "elements": [
                                                    { "typeElement": "SOL",      "etatElement": "USAGE_NORMAL", "description": "Parquet chêne",               "observation": "Plusieurs griffures légères côté canapé" },
                                                    { "typeElement": "MUR",      "etatElement": "MAUVAIS",      "description": "Peinture blanche",            "observation": "Trou ~2 cm côté TV + marques de fixation" },
                                                    { "typeElement": "PLAFOND",  "etatElement": "BON",          "description": "Peinture blanche",            "observation": null },
                                                    { "typeElement": "FENETRE",  "etatElement": "BON",          "description": "Double vitrage PVC",          "observation": null },
                                                    { "typeElement": "VOLET",    "etatElement": "HORS_SERVICE", "description": "Volet roulant électrique",    "observation": "Moteur en panne — ne remonte plus" },
                                                    { "typeElement": "RADIATEUR","etatElement": "BON",          "description": "Radiateur électrique inertie","observation": null },
                                                    { "typeElement": "PRISE",    "etatElement": "BON",          "description": "4 prises + 1 TV",             "observation": null },
                                                    { "typeElement": "LUMINAIRE","etatElement": "BON",          "description": "Plafonnier + applique",       "observation": null }
                                                  ]
                                                },
                                                {
                                                  "nomPiece": "Cuisine", "ordre": 3, "observations": "Plaque induction avec fissure constatée.",
                                                  "elements": [
                                                    { "typeElement": "SOL",       "etatElement": "USAGE_NORMAL", "description": "Carrelage blanc",         "observation": "Joint décollé entre 2 carreaux" },
                                                    { "typeElement": "MUR",       "etatElement": "BON",          "description": "Faïence + peinture",      "observation": null },
                                                    { "typeElement": "PLAFOND",   "etatElement": "BON",          "description": "Peinture blanche",        "observation": null },
                                                    { "typeElement": "EQUIPEMENT","etatElement": "MAUVAIS",      "description": "Plaque induction 4 feux", "observation": "Fissure sur la vitre — non présente à l'entrée" },
                                                    { "typeElement": "EQUIPEMENT","etatElement": "BON",          "description": "Four encastrable",        "observation": null },
                                                    { "typeElement": "EQUIPEMENT","etatElement": "USAGE_NORMAL", "description": "Hotte aspirante",         "observation": "Filtre non changé depuis l'entrée" },
                                                    { "typeElement": "EQUIPEMENT","etatElement": "BON",          "description": "Réfrigérateur combiné",   "observation": null },
                                                    { "typeElement": "LUMINAIRE", "etatElement": "BON",          "description": "Spots encastrés LED",     "observation": null }
                                                  ]
                                                },
                                                {
                                                  "nomPiece": "WC", "ordre": 4, "observations": "Chasse d'eau réparée entre l'entrée et la sortie — OK.",
                                                  "elements": [
                                                    { "typeElement": "SOL",       "etatElement": "BON", "description": "Carrelage gris",   "observation": null },
                                                    { "typeElement": "MUR",       "etatElement": "BON", "description": "Peinture blanche", "observation": null },
                                                    { "typeElement": "EQUIPEMENT","etatElement": "BON", "description": "WC suspendu",      "observation": "Chasse d'eau réparée — conforme" },
                                                    { "typeElement": "LUMINAIRE", "etatElement": "BON", "description": "Plafonnier",       "observation": null }
                                                  ]
                                                }
                                              ]
                                            }
                                            """
                            )
                    }
            )
    )
    @PostMapping
    public ResponseEntity<Void> creerEtatDesLieux(
            @Valid @RequestBody EtatDesLieuxFormDTO dto
    ) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        edlService.creerEtatDesLieux(dto, auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }


    // =========================================
    // SIGNATURE PROPRIÉTAIRE
    // =========================================
    @Operation(
            summary = "Signer l'état des lieux — propriétaire",
            description = """
                    Permet au propriétaire de signer l'état des lieux.
                    La signature est transmise en base64 (image PNG capturée depuis le canvas).
                    Une fois signé, le PDF est régénéré avec la signature intégrée
                    et un email est envoyé au locataire avec le lien de signature.
                    L'EDL passe au statut `EN_ATTENTE_SIGNATURE_LOCATAIRE`.
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "204",
                    description = "Signature enregistrée — email d'invitation envoyé au locataire"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Signature manquante ou invalide",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    { "error": "La signature est obligatoire" }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "L'utilisateur n'est pas le propriétaire de cet état des lieux",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    { "error": "Accès refusé" }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "État des lieux introuvable",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    { "error": "État des lieux introuvable : 1" }
                                    """)
                    )
            )
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Signature du propriétaire en base64",
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(value = """
                            {
                                "signatureBase64": "iVBORw0KGgoAAAANSUhEUgAA..."
                            }
                            """)
            )
    )
    @PostMapping("/{id}/signer-proprietaire")
    public ResponseEntity<Void> signerProprietaire(
            @Parameter(description = "Identifiant de l'état des lieux", required = true)
            @PathVariable Long id,
            @Valid @RequestBody SignatureDTO dto
    ) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        edlService.signerProprietaire(id, dto.signatureBase64(), auth.getName());
        return ResponseEntity.noContent().build();
    }


    // =========================================
    // PAGE SIGNATURE LOCATAIRE (GET)
    // =========================================
    @Operation(
            summary = "Consulter l'état des lieux à signer — locataire",
            description = """
                    Endpoint appelé par le front lorsque le locataire clique sur le lien reçu par email.
                    Retourne les informations de l'état des lieux associé au token pour afficher
                    un récapitulatif complet (pièces, éléments, compteurs, clés) avant signature.
                    Le token est valable **72 heures** — passé ce délai l'EDL passe
                    au statut `EXPIRE` et une erreur `401` est retournée.
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "État des lieux trouvé — données retournées pour affichage",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = EtatDesLieuxDTO.class),
                            examples = @ExampleObject(value = """
                                    {
                                        "id": 1,
                                        "type": "ENTREE",
                                        "statut": "EN_ATTENTE_SIGNATURE_LOCATAIRE",
                                        "dateRealisation": "2026-03-19",
                                        "heureRealisation": "09:30",
                                        "adresseBien": "75 Boulevard Jules Verne, 44000 Nantes",
                                        "typeBien": "APPARTEMENT",
                                        "nomProprietaire": "Jean Dupont",
                                        "emailProprietaire": "jean.dupont@email.com",
                                        "nomLocataire": "Marie Martin",
                                        "emailLocataire": "moiseaganze76@gmail.com",
                                        "observations": "État général satisfaisant.",
                                        "urlPdf": "https://minio.kupanga.com/edl/edl_entree_1_2026-03-19.pdf"
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Token invalide ou expiré",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    { "error": "Token invalide" }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "L'état des lieux n'est pas en attente de signature locataire",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    { "error": "Cet état des lieux n'est pas disponible à la signature — statut actuel : SIGNE" }
                                    """)
                    )
            )
    })
    @GetMapping("/signer/{token}")
    public ResponseEntity<EtatDesLieuxDTO> getEdlParToken(
            @Parameter(description = "Token de signature reçu par email", required = true)
            @PathVariable String token
    ) {
        return ResponseEntity.ok(edlService.getEdlParToken(token));
    }


    // =========================================
    // SIGNATURE LOCATAIRE (POST)
    // =========================================
    @Operation(
            summary = "Soumettre la signature — locataire",
            description = """
                    Permet au locataire de soumettre sa signature.
                    La signature est transmise en base64 (image PNG capturée depuis le canvas).
                    Une fois les deux signatures présentes, le PDF final est généré
                    et un email de confirmation est envoyé aux **deux parties**
                    avec l'état des lieux signé en pièce jointe.
                    L'EDL passe au statut `SIGNE` et le token est invalidé.
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "204",
                    description = "Signature enregistrée — emails de confirmation envoyés aux deux parties"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Signature manquante ou EDL non signable",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    { "error": "La signature est obligatoire" }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Token invalide ou expiré — le locataire doit contacter le propriétaire",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    { "error": "Le lien de signature a expiré" }
                                    """)
                    )
            )
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Signature du locataire en base64",
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(value = """
                            {
                                "signatureBase64": "iVBORw0KGgoAAAANSUhEUgAA..."
                            }
                            """)
            )
    )
    @PostMapping("/signer/{token}")
    public ResponseEntity<Void> signerLocataire(
            @Parameter(description = "Token de signature reçu par email", required = true)
            @PathVariable String token,
            @Valid @RequestBody SignatureDTO dto
    ) {
        edlService.signerLocataire(token, dto.signatureBase64());
        return ResponseEntity.noContent().build();
    }
}