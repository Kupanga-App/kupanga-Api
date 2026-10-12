
// ─────────────────────────────────────────────────────────────────────────────
// MessagePayload.java  — payload reçu via WebSocket depuis le front
// ─────────────────────────────────────────────────────────────────────────────
package com.kupanga.api.chat.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record MessagePayload (

    @NotBlank(message = "Le contenu du message ne doit pas être vide.")
    @Size(max = 4000, message = "Le message ne doit pas dépasser 4000 caractères.")
    String contenu,

    // Facultatif : absent pour un premier message depuis une annonce → propriétaire du bien
    @Email(message = "L'e-mail du destinataire doit être valide.")
    @Size(max = 255)
    String emailDestinataire,

    @NotNull(message = "l'id du bien concerné est obligatoire")
    Long bienId
){}
