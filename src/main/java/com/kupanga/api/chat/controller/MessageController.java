package com.kupanga.api.chat.controller;

import com.kupanga.api.chat.dto.MessageDTO;
import com.kupanga.api.chat.dto.MessagePayload;
import com.kupanga.api.chat.service.MessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
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
    // REST — historique de conversation
    // ─────────────────────────────────────────────────────────────────────────

    @Operation(
            summary = "Récupérer une conversation",
            description = """
                    Retourne l'historique complet des messages échangés entre
                    l'utilisateur connecté et un autre utilisateur,
                    triés par date croissante.
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Conversation retournée"),
            @ApiResponse(responseCode = "401", description = "Non authentifié")
    })
    @GetMapping("/messages/conversation/{emailInterlocuteur}")
    public ResponseEntity<List<MessageDTO>> getConversation(
            @Parameter(description = "Email de l'interlocuteur", required = true)
            @PathVariable String emailInterlocuteur
    ) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return ResponseEntity.ok(
                messageService.getConversation(auth.getName(), emailInterlocuteur));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // REST — conversation dans le contexte d'un bien
    // ─────────────────────────────────────────────────────────────────────────

    @Operation(
            summary = "Récupérer une conversation liée à un bien",
            description = """
                    Retourne les messages échangés entre l'utilisateur connecté
                    et un interlocuteur dans le contexte d'un bien spécifique.
                    Utile pour afficher la conversation proprio ↔ locataire
                    directement depuis la fiche d'un bien.
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Conversation retournée"),
            @ApiResponse(responseCode = "401", description = "Non authentifié"),
            @ApiResponse(responseCode = "404", description = "Bien introuvable")
    })
    @GetMapping("/messages/conversation/{emailInterlocuteur}/bien/{bienId}")
    public ResponseEntity<List<MessageDTO>> getConversationParBien(
            @Parameter(description = "Email de l'interlocuteur", required = true)
            @PathVariable String emailInterlocuteur,
            @Parameter(description = "Identifiant du bien", required = true)
            @PathVariable Long bienId
    ) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return ResponseEntity.ok(
                messageService.getConversationParBien(auth.getName(), emailInterlocuteur, bienId));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // REST — marquer une conversation comme lue
    // ─────────────────────────────────────────────────────────────────────────

    @Operation(
            summary = "Marquer une conversation comme lue",
            description = """
                    Marque tous les messages non lus d'une conversation comme lus.
                    À appeler dès que l'utilisateur ouvre la conversation.
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Messages marqués comme lus"),
            @ApiResponse(responseCode = "401", description = "Non authentifié")
    })
    @PostMapping("/messages/conversation/{emailExpediteur}/lire")
    public ResponseEntity<Void> marquerLue(
            @Parameter(description = "Email de l'expéditeur de la conversation", required = true)
            @PathVariable String emailExpediteur
    ) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        messageService.marquerConversationLue(auth.getName(), emailExpediteur);
        return ResponseEntity.noContent().build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // REST — compteur de messages non lus
    // ─────────────────────────────────────────────────────────────────────────

    @Operation(
            summary = "Nombre de messages non lus",
            description = "Retourne le nombre total de messages non lus pour l'utilisateur connecté.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Compteur retourné",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(value = "{ \"nonLus\": 3 }")
                            )
                    )
            }
    )
    @GetMapping("/messages/non-lus")
    public ResponseEntity<Long> countNonLus() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return ResponseEntity.ok(messageService.countMessagesNonLus(auth.getName()));
    }
}
