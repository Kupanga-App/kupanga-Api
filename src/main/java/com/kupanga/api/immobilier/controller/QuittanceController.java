package com.kupanga.api.immobilier.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import com.kupanga.api.immobilier.dto.formDTO.QuittanceFormDTO;
import com.kupanga.api.immobilier.dto.formDTO.MarquerPayeeDTO;
import com.kupanga.api.immobilier.dto.readDTO.QuittanceDTO;
import com.kupanga.api.immobilier.research.QuittanceSearchService;
import com.kupanga.api.immobilier.research.dto.QuittancePageDTO;
import com.kupanga.api.immobilier.research.dto.QuittanceSearchDTO;
import com.kupanga.api.immobilier.service.QuittanceService;
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
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/quittances")
@RequiredArgsConstructor
@Tag(name = "Quittances", description = "Gestion des quittances de loyer — création, paiement et consultation")
public class QuittanceController {

    private final QuittanceService quittanceService;
    private final QuittanceSearchService quittanceSearchService;

    // =========================================
    // CRÉER UNE QUITTANCE
    // =========================================
    @Operation(
            summary = "Créer une quittance de loyer",
            description = """
                    Permet à un propriétaire authentifié de créer une quittance pour un mois donné.
                    Si un `contratId` est fourni, le loyer et les charges sont récupérés
                    automatiquement depuis le contrat — sinon `loyerMensuel` et `chargesMensuelles`
                    sont obligatoires dans le body.
                    Le PDF est généré automatiquement et stocké sur MinIO.
                    La quittance est créée au statut `EN_ATTENTE` — elle passera à `PAYEE`
                    uniquement via l'endpoint `/marquer-payee` avec la signature du propriétaire.
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Quittance créée avec succès"),
            @ApiResponse(
                    responseCode = "409",
                    description = "Une quittance existe déjà pour ce bien / mois / année",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    { "error": "Une quittance existe déjà pour ce bien en 3/2026" }
                                    """))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Données invalides ou loyer/charges manquants sans contrat",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    { "error": "Loyer et charges obligatoires si aucun contrat n'est fourni" }
                                    """))
            ),
            @ApiResponse(responseCode = "401", description = "Non authentifié"),
            @ApiResponse(responseCode = "404", description = "Bien, locataire ou contrat introuvable")
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Données de la quittance à créer",
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    examples = {
                            @ExampleObject(
                                    name = "Avec contrat (loyer automatique)",
                                    summary = "Quittance liée à un contrat existant — loyer et charges récupérés automatiquement",
                                    value = """
                                            {
                                              "bienId": 1,
                                              "emailLocataire": "moiseaganze76@gmail.com",
                                              "contratId": 1,
                                              "mois": 3,
                                              "annee": 2026,
                                              "dateEcheance": "2026-03-05"
                                            }
                                            """
                            ),
                            @ExampleObject(
                                    name = "Sans contrat (loyer manuel)",
                                    summary = "Quittance avec loyer et charges saisis manuellement",
                                    value = """
                                            {
                                              "bienId": 1,
                                              "emailLocataire": "moiseaganze76@gmail.com",
                                              "mois": 3,
                                              "annee": 2026,
                                              "loyerMensuel": 850.00,
                                              "chargesMensuelles": 50.00,
                                              "dateEcheance": "2026-03-05"
                                            }
                                            """
                            )
                    }
            )
    )
    @PreAuthorize("hasAuthority('ROLE_PROPRIETAIRE')")
    @PostMapping
    public ResponseEntity<Void> creerQuittance(
            @Valid @RequestBody QuittanceFormDTO dto
    ) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        quittanceService.creerQuittance(dto, auth.getName());
        return ResponseEntity.noContent().build();
    }


    // =========================================
    // MARQUER PAYÉE + SIGNATURE
    // =========================================
    @Operation(
            summary = "Marquer une quittance comme payée — avec signature",
            description = """
                    Permet au propriétaire d'enregistrer le paiement d'une quittance
                    et d'y apposer sa signature manuscrite (capturée depuis un canvas en base64).
                    
                    À la réception :
                    - La date de paiement est automatiquement fixée à aujourd'hui
                    - La signature est stockée et intégrée au PDF
                    - Le PDF est régénéré avec la signature et la date de paiement
                    - Un email est envoyé au locataire avec le PDF en pièce jointe
                    - La quittance passe au statut `PAYEE`
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "204",
                    description = "Quittance signée et marquée payée — email envoyé au locataire avec le PDF"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Quittance déjà payée ou signature invalide",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    { "error": "Cette quittance est déjà marquée comme payée" }
                                    """))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Non authentifié ou utilisateur non propriétaire de cette quittance",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    { "error": "Accès non autorisé" }
                                    """))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Quittance introuvable",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    { "error": "Quittance introuvable : 1" }
                                    """))
            )
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "C8-bis : signature du propriétaire (PNG en base64), mode de paiement (proposé dans le pays, "
                    + "cf. GET /juridictions/{pays}), date de réception (facultative, aujourd'hui sinon, jamais dans le futur) "
                    + "et référence facultative. Aucun paiement ne passe par l'application.",
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(value = """
                            {
                                "signatureBase64": "iVBORw0KGgoAAAANSUhEUgAA...",
                                "modePaiement": "MOBILE_MONEY",
                                "datePaiement": "2026-10-05",
                                "referencePaiement": "MP261005.1432.A12345"
                            }
                            """)
            )
    )
    @PreAuthorize("hasAuthority('ROLE_PROPRIETAIRE')")
    @PostMapping("/{id}/marquer-payee")
    public ResponseEntity<Void> marquerPayee(
            @Parameter(description = "Identifiant de la quittance", required = true)
            @PathVariable Long id,
            @Valid @RequestBody MarquerPayeeDTO dto
    ) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        quittanceService.marquerPayee(id, dto, auth.getName());
        return ResponseEntity.noContent().build();
    }

    // =========================================
    // C8-BIS : PHOTO DU REÇU
    // =========================================
    @Operation(
            summary = "Joindre la photo du reçu de paiement",
            description = """
                    Ajoute ou remplace la photo du reçu (espèces, Mobile Money…) d'une quittance **déjà payée**.
                    Facultatif. Formats des smartphones (JPEG, PNG, WEBP, AVIF, GIF, HEIC/HEIF), 10 Mo au plus,
                    reconnus au contenu. Stockée dans un bucket privé ; lue par une URL présignée de 5 minutes
                    (`urlRecu`), visible du propriétaire et du locataire. Aucun paiement ne passe par l'application.
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Reçu joint, quittance renvoyée",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = QuittanceDTO.class))),
            @ApiResponse(responseCode = "400", description = "Fichier absent ou vide"),
            @ApiResponse(responseCode = "401", description = "Non authentifié"),
            @ApiResponse(responseCode = "403", description = "Pas propriétaire de cette quittance"),
            @ApiResponse(responseCode = "404", description = "Quittance introuvable"),
            @ApiResponse(responseCode = "409", description = "Quittance non payée, bien archivé ou compte supprimé"),
            @ApiResponse(responseCode = "413", description = "Fichier trop volumineux"),
            @ApiResponse(responseCode = "415", description = "Format d'image non accepté")
    })
    @PreAuthorize("hasAuthority('ROLE_PROPRIETAIRE')")
    @PostMapping(value = "/{id}/recu", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<QuittanceDTO> ajouterRecu(
            @Parameter(description = "Identifiant de la quittance", required = true)
            @PathVariable Long id,
            @RequestPart("recu") MultipartFile recu
    ) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return ResponseEntity.ok(quittanceService.ajouterRecu(id, recu, auth.getName()));
    }


    // =========================================
    // LISTE PAR BIEN (propriétaire)
    // =========================================
    @Operation(
            summary = "Lister les quittances d'un bien",
            description = "Retourne toutes les quittances d'un bien appartenant au propriétaire connecté."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Liste des quittances",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = QuittanceDTO.class),
                            examples = @ExampleObject(value = """
                                    [
                                      {
                                        "id": 1,
                                        "mois": 3,
                                        "annee": 2026,
                                        "moisLabel": "Mars 2026",
                                        "loyerMensuel": 850.00,
                                        "chargesMensuelles": 50.00,
                                        "montantTotal": 900.00,
                                        "dateEcheance": "2026-03-05",
                                        "datePaiement": "2026-03-19",
                                        "statut": "PAYEE",
                                        "nomLocataire": "Moise Aganze",
                                        "adresseBien": "75 Boulevard Jules Verne, 44000 Nantes"
                                      }
                                    ]
                                    """))
            ),
            @ApiResponse(responseCode = "401", description = "Non authentifié"),
            @ApiResponse(responseCode = "404", description = "Bien introuvable")
    })
    @PreAuthorize("hasAuthority('ROLE_PROPRIETAIRE')")
    @GetMapping("/bien/{bienId}")
    public ResponseEntity<List<QuittanceDTO>> getQuittancesParBien(
            @Parameter(description = "Identifiant du bien", required = true)
            @PathVariable Long bienId
    ) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return ResponseEntity.ok(quittanceService.getQuittancesParBien(bienId, auth.getName()));
    }


    // =========================================
    // MES QUITTANCES (locataire)
    // =========================================
    @Operation(
            summary = "Mes quittances — locataire",
            description = "Retourne toutes les quittances du locataire connecté, tous biens confondus."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Liste des quittances du locataire"),
            @ApiResponse(responseCode = "401", description = "Non authentifié")
    })
    @PreAuthorize("hasAuthority('ROLE_LOCATAIRE')")
    @GetMapping("/mes-quittances")
    public ResponseEntity<List<QuittanceDTO>> getMesQuittances() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return ResponseEntity.ok(quittanceService.getQuittancesParLocataire(auth.getName()));
    }


    // =========================================
    // DÉTAIL PAR ID
    // =========================================
    @Operation(
            summary = "Détail d'une quittance",
            description = """
                    Retourne le détail complet d'une quittance.
                    Accessible par le propriétaire **ou** le locataire concerné.
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Quittance trouvée",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = QuittanceDTO.class))
            ),
            @ApiResponse(responseCode = "401", description = "Non authentifié ou accès non autorisé"),
            @ApiResponse(responseCode = "404", description = "Quittance introuvable")
    })
    @GetMapping("/{id}")
    public ResponseEntity<QuittanceDTO> getQuittanceById(
            @Parameter(description = "Identifiant de la quittance", required = true)
            @PathVariable Long id
    ) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return ResponseEntity.ok(quittanceService.getQuittanceById(id, auth.getName()));
    }

    // =========================================
    // RECHERCHE PAGINÉE (utilisateur connecté)
    // =========================================
    @Operation(
            summary = "Rechercher ses quittances",
            description = """
                    Retourne les quittances de l'utilisateur connecté, paginées et filtrables.
                    Un propriétaire voit les quittances de ses biens,
                    un locataire voit les siennes.
                    Tri par défaut : année + mois décroissants.
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Liste paginée des quittances"),
            @ApiResponse(responseCode = "401", description = "Non authentifié")
    })
    @PostMapping("/search")
    public ResponseEntity<QuittancePageDTO> search(
            @Valid @RequestBody QuittanceSearchDTO dto
    ) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return ResponseEntity.ok(quittanceSearchService.rechercher(dto, auth.getName()));
    }


}