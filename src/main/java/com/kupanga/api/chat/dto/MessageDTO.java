package com.kupanga.api.chat.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder(toBuilder = true)
public class MessageDTO {

    private Long          id;
    private String        contenu;
    private Boolean       lu;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
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

    // ─── Accusé d'envoi (W13) ─────────────────────────────────────────────────
    // Identifiant choisi par le front (en-tête STOMP id-client), renvoyé seulement à l'expéditeur
    // dans l'écho de son message ; non persisté
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String        idClient;
}
