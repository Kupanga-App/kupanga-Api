package com.kupanga.api.chat.controller;

import com.kupanga.api.chat.dto.MessageDTO;
import com.kupanga.api.chat.dto.MessagePayload;
import com.kupanga.api.chat.service.MessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Messagerie", description = "Messagerie temps réel entre propriétaire et locataire")
public class MessageController {

    private final MessageService messageService;

    // ─────────────────────────────────────────────────────────────────────────
    // WebSocket — envoi d'un message en temps réel
    // Destination : /app/chat.send
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Endpoint WebSocket STOMP.
     * Le front envoie sur /app/chat.send
     * Le destinataire reçoit sur /user/{email}/queue/messages
     */
    @MessageMapping("/chat.send")
    public void sendMessage(@Payload MessagePayload payload, Principal principal) {
        messageService.envoyerMessage(payload, principal.getName());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Historique d'une conversation
    // ─────────────────────────────────────────────────────────────────────────
    @Operation(
            summary = "Historique d'une conversation",
            description = """
                    Retourne la liste chronologique des messages échangés entre l'utilisateur
                    connecté et un interlocuteur donné, dans le contexte d'un bien précis.
                    Les messages sont triés du plus ancien au plus récent.
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Historique retourné avec succès",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = MessageDTO.class),
                            examples = @ExampleObject(value = """
                                    [
                                        {
                                            "id": 1,
                                            "contenu": "Bonjour, est-il possible de visiter samedi ?",
                                            "lu": true,
                                            "createdAt": "2026-05-17T14:32:00",
                                            "expediteurId": 12,
                                            "expediteurNom": "Sophie Martin",
                                            "expediteurEmail": "sophie.martin@email.com",
                                            "destinataireId": 3,
                                            "destinataireNom": "Jean Dupont",
                                            "destinataireEmail": "jean.dupont@email.com",
                                            "bienId": 1,
                                            "bienAdresse": "75 Boulevard Jules Verne"
                                        }
                                    ]
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
            )
    })
    @GetMapping("/historique")
    public ResponseEntity<List<MessageDTO>> getHistorique(
            @Parameter(description = "Identifiant du bien concerné par la conversation", required = true)
            @RequestParam Long bienId,
            @Parameter(description = "Email de l'interlocuteur (propriétaire ou locataire)", required = true)
            @RequestParam String emailInterlocuteur,
            Principal principal) {

        List<MessageDTO> messages = messageService.getHistorique(
                bienId,
                principal.getName(),
                emailInterlocuteur
        );
        return ResponseEntity.ok(messages);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Compteur de messages non lus
    // ─────────────────────────────────────────────────────────────────────────
    @Operation(
            summary = "Nombre de messages non lus",
            description = """
                    Retourne le nombre total de messages non lus pour l'utilisateur connecté,
                    toutes conversations confondues. Utile pour afficher un badge dans la navigation.
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Nombre de messages non lus",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = "3")
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
    @GetMapping("/messages/non-lus")
    public ResponseEntity<Long> countNonLus(Principal principal) {
        return ResponseEntity.ok(messageService.countMessagesNonLus(principal.getName()));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Marquer une conversation comme lue
    // ─────────────────────────────────────────────────────────────────────────
    @Operation(
            summary = "Marquer une conversation comme lue",
            description = """
                    Marque comme lus tous les messages reçus de l'expéditeur donné
                    dans la conversation avec l'utilisateur connecté.
                    À appeler dès que l'utilisateur ouvre une conversation.
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Conversation marquée comme lue"
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
    @PostMapping("/messages/conversation/{emailExpediteur}/lire")
    public ResponseEntity<Void> marquerLus(
            @Parameter(description = "Email de l'expéditeur dont on veut marquer les messages comme lus", required = true)
            @PathVariable String emailExpediteur,
            Principal principal) {
        messageService.marquerConversationLue(principal.getName(), emailExpediteur);
        return ResponseEntity.ok().build();
    }
}
