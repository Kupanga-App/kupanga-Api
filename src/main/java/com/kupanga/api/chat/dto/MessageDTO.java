package com.kupanga.api.chat.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class MessageDTO {

    private Long          id;
    private String        contenu;
    private Boolean       lu;
    private LocalDateTime createdAt;

    // ─── Expéditeur ───────────────────────────────────────────────────────────
    private Long          expediteurId;
    private String        expediteurNom;
    private String        expediteurEmail;

    // ─── Destinataire ─────────────────────────────────────────────────────────
    private Long          destinataireId;
    private String        destinataireNom;
    private String        destinataireEmail;

    // ─── Contexte bien (optionnel) ────────────────────────────────────────────
    private Long          bienId;
    private String        bienAdresse;
}
