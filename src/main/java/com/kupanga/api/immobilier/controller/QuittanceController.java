package com.kupanga.api.immobilier.controller;

import com.kupanga.api.immobilier.dto.formDTO.QuittanceFormDTO;
import com.kupanga.api.immobilier.dto.formDTO.SignatureDTO;
import com.kupanga.api.immobilier.dto.readDTO.QuittanceDTO;
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
import org.springframework.http.ResponseEntity;
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
            description = "Signature du propriétaire en base64 (image PNG capturée depuis le canvas)",
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
    @PostMapping("/{id}/marquer-payee")
    public ResponseEntity<Void> marquerPayee(
            @Parameter(description = "Identifiant de la quittance", required = true)
            @PathVariable Long id,
            @Valid @RequestBody SignatureDTO dto
    ) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        quittanceService.marquerPayee(id, dto.signatureBase64(), auth.getName());
        return ResponseEntity.noContent().build();
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
}