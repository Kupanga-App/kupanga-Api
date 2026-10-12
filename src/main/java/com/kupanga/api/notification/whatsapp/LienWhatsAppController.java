package com.kupanga.api.notification.whatsapp;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * C7-C16 : liens WhatsApp {@code wa.me} préremplis, ouverts par le propriétaire depuis son téléphone.
 * Rien n'est envoyé par l'API.
 */
@RestController
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_PROPRIETAIRE')")
@Tag(name = "WhatsApp", description = "Messages WhatsApp préremplis que le propriétaire envoie lui-même (pays avec le canal WHATSAPP_LIEN)")
public class LienWhatsAppController {

    private static final String EXEMPLE = """
            {
              "url": "https://wa.me/243812345678?text=Bonjour%20Grace%2C%20j%27ai%20sign%C3%A9%20le%20bail...",
              "texte": "Bonjour Grace, j'ai signé le bail du logement situé ...",
              "numeroConnu": true
            }
            """;

    private final LienWhatsAppService lienWhatsAppService;

    @Operation(
            summary = "Invitation WhatsApp à signer un bail",
            description = """
                    Message prérempli pour prévenir le locataire qu'un bail attend sa signature. Le message ne contient
                    **pas** le lien de signature : le locataire se connecte et signe depuis l'invitation reçue
                    (notification ou e-mail). Sans numéro du locataire, `url` ouvre WhatsApp sur le choix du contact.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lien préparé",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = EXEMPLE))),
            @ApiResponse(responseCode = "401", description = "Non authentifié"),
            @ApiResponse(responseCode = "403", description = "Pas propriétaire de ce contrat"),
            @ApiResponse(responseCode = "404", description = "Contrat introuvable"),
            @ApiResponse(responseCode = "409", description = "Pays sans WhatsApp, contrat qui n'attend pas la signature du locataire (ou lien expiré), bien archivé ou compte supprimé")
    })
    @GetMapping("/contrats/{id}/lien-whatsapp")
    public ResponseEntity<LienWhatsAppDTO> invitationContrat(
            @Parameter(description = "Identifiant du contrat") @PathVariable Long id) {
        return ResponseEntity.ok(lienWhatsAppService.invitationContrat(id, emailConnecte()));
    }

    @Operation(
            summary = "Invitation WhatsApp à signer un état des lieux",
            description = "Comme pour le bail : sans lien de signature, le locataire signe depuis l'invitation reçue."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lien préparé",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = EXEMPLE))),
            @ApiResponse(responseCode = "401", description = "Non authentifié"),
            @ApiResponse(responseCode = "403", description = "Pas propriétaire de cet état des lieux"),
            @ApiResponse(responseCode = "404", description = "État des lieux introuvable"),
            @ApiResponse(responseCode = "409", description = "Pays sans WhatsApp, état des lieux qui n'attend pas la signature du locataire (ou lien expiré), bien archivé ou compte supprimé")
    })
    @GetMapping("/etats-des-lieux/{id}/lien-whatsapp")
    public ResponseEntity<LienWhatsAppDTO> invitationEtatDesLieux(
            @Parameter(description = "Identifiant de l'état des lieux") @PathVariable Long id) {
        return ResponseEntity.ok(lienWhatsAppService.invitationEtatDesLieux(id, emailConnecte()));
    }

    @Operation(
            summary = "Rappel de loyer par WhatsApp",
            description = "Message prérempli rappelant au locataire un loyer non payé (mois, logement, montant). Aucun paiement ne passe par l'application."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lien préparé",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = EXEMPLE))),
            @ApiResponse(responseCode = "401", description = "Non authentifié"),
            @ApiResponse(responseCode = "403", description = "Pas propriétaire de cette quittance"),
            @ApiResponse(responseCode = "404", description = "Quittance introuvable"),
            @ApiResponse(responseCode = "409", description = "Pays sans WhatsApp, loyer déjà payé, bien archivé ou compte supprimé")
    })
    @GetMapping("/quittances/{id}/lien-whatsapp")
    public ResponseEntity<LienWhatsAppDTO> rappelLoyer(
            @Parameter(description = "Identifiant de la quittance") @PathVariable Long id) {
        return ResponseEntity.ok(lienWhatsAppService.rappelLoyer(id, emailConnecte()));
    }

    private static String emailConnecte() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
