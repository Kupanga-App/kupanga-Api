package com.kupanga.api.immobilier.controller;

import com.kupanga.api.immobilier.dto.formDTO.ContratFormDTO;
import com.kupanga.api.immobilier.dto.formDTO.SignatureDTO;
import com.kupanga.api.immobilier.dto.readDTO.ContratDTO;
import com.kupanga.api.immobilier.service.ContratService;
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

@RestController
@RequestMapping("/contrats")
@RequiredArgsConstructor
@Tag(name = "Contrats", description = "Gestion des contrats de location et signatures électroniques")
public class ContratController {

    private final ContratService contratService;

    // =========================================
    // CRÉER UN CONTRAT
    // =========================================
    @Operation(
            summary = "Créer un contrat de location",
            description = """
                    Permet à un propriétaire authentifié de créer un contrat de location
                    pour un de ses biens. Le PDF du contrat est généré automatiquement
                    et stocké sur MinIO. Le contrat passe au statut
                    `EN_ATTENTE_SIGNATURE_PROPRIO` après création.
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "204",
                    description = "Contrat créé avec succès"
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
                                            "loyerMensuel": "Le loyer mensuel est obligatoire",
                                            "dateFin": "La date de fin doit être après la date de début"
                                        },
                                        "timestamp": "2026-03-16T00:00:00"
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
                    responseCode = "403",
                    description = "L'utilisateur n'est pas le propriétaire du bien",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    { "error": "Accès refusé : rôle PROPRIETAIRE requis" }
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
            description = "Informations du contrat à créer",
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(value = """
                            {
                                "bienId": 1,
                                "emailLocataire": "sophie.martin@email.com",
                                "dateDebut": "2026-04-01",
                                "dateFin": "2027-04-01",
                                "dureeBailMois": 12,
                                "loyerMensuel": 850.00,
                                "chargesMensuelles": 50.00,
                                "depotGarantie": 1700.00
                            }
                            """)
            )
    )
    @PostMapping
    public ResponseEntity<Void> creerContrat(
            @Valid @RequestBody ContratFormDTO dto
    ) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        contratService.creerContrat(dto, auth.getName());
        return ResponseEntity.noContent().build();
    }


    // =========================================
    // SIGNATURE PROPRIÉTAIRE
    // =========================================
    @Operation(
            summary = "Signer le contrat — propriétaire",
            description = """
                    Permet au propriétaire de signer le contrat.
                    La signature est transmise en base64 (image PNG capturée depuis le canvas).
                    Une fois signé, le PDF est régénéré avec la signature intégrée
                    et un email est envoyé au locataire avec le lien de signature.
                    Le contrat passe au statut `EN_ATTENTE_SIGNATURE_LOCATAIRE`.
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "204",
                    description = "Signature enregistrée — email envoyé au locataire"
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
                    description = "L'utilisateur n'est pas le propriétaire de ce contrat",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    { "error": "Accès refusé" }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Contrat introuvable",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    { "error": "Contrat introuvable : 1" }
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
    @PostMapping("/{id}/signer-proprio")
    public ResponseEntity<Void> signerProprietaire(
            @Parameter(description = "Identifiant du contrat", required = true)
            @PathVariable Long id,
            @Valid @RequestBody SignatureDTO dto
    ) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        contratService.signerProprietaire(id, dto.signatureBase64(), auth.getName());
        return ResponseEntity.noContent().build();
    }


    // =========================================
    // PAGE SIGNATURE LOCATAIRE (GET)
    // =========================================
    @Operation(
            summary = "Consulter le contrat à signer — locataire",
            description = """
                    Endpoint appelé par le front lorsque le locataire clique sur le lien reçu par email.
                    Retourne les informations du contrat associé au token pour afficher
                    un récapitulatif avant signature.
                    Le token est valable **72 heures** — passé ce délai le contrat passe
                    au statut `EXPIRE` et une erreur `410 Gone` est retournée.
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Contrat trouvé — données retournées pour affichage",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ContratDTO.class),
                            examples = @ExampleObject(value = """
                                    {
                                        "id": 1,
                                        "adresseBien": "75 Boulevard Jules Verne, Nantes",
                                        "loyerMensuel": 850.00,
                                        "chargesMensuelles": 50.00,
                                        "depotGarantie": 1700.00,
                                        "dateDebut": "2026-04-01",
                                        "dureeBailMois": 12,
                                        "statut": "EN_ATTENTE_SIGNATURE_LOCATAIRE"
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Token invalide — contrat introuvable",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    { "error": "Token invalide" }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "410",
                    description = "Token expiré — le lien n'est plus valide",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    { "error": "Le lien de signature a expiré" }
                                    """)
                    )
            )
    })
    @GetMapping("/signer/{token}")
    public ResponseEntity<ContratDTO> getContratParToken(
            @Parameter(description = "Token de signature reçu par email", required = true)
            @PathVariable String token
    ) {
        return ResponseEntity.ok(contratService.getContratParToken(token));
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
                    avec le contrat signé en pièce jointe.
                    Le contrat passe au statut `SIGNE`.
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "204",
                    description = "Signature enregistrée — emails de confirmation envoyés aux deux parties"
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
                    responseCode = "404",
                    description = "Token invalide",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    { "error": "Token invalide" }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "410",
                    description = "Token expiré — le locataire doit contacter le propriétaire",
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
        contratService.signerLocataire(token, dto.signatureBase64());
        return ResponseEntity.noContent().build();
    }
}