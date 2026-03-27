
// ─────────────────────────────────────────────────────────────────────────────
// MessagePayload.java  — payload reçu via WebSocket depuis le front
// ─────────────────────────────────────────────────────────────────────────────
package com.kupanga.api.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MessagePayload {

    @NotBlank
    private String contenu;

    @NotNull
    private String emailDestinataire;   // email du destinataire

    private Long bienId;                // optionnel — contexte du bien concerné
}
