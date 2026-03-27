
// ─────────────────────────────────────────────────────────────────────────────
// MessagePayload.java  — payload reçu via WebSocket depuis le front
// ─────────────────────────────────────────────────────────────────────────────
package com.kupanga.api.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record MessagePayload (

    @NotBlank(message = "Le contenu du message ne doit pas être vide.")
    String contenu,

    @NotBlank(message = "l'email du destinataire ne doit pas être vide.")
    String emailDestinataire,

    @NotNull(message = "l'id du bien concerné est obligatoire")
    Long bienId
){}
