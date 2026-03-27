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
}
