package com.kupanga.api.immobilier.dto.readDTO;

import com.kupanga.api.immobilier.entity.*;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Builder
/**
 * Vue publique d'un bien ({@code GET /biens/{id}}, {@code POST /biens/search}) — P0-6.
 * Aucune donnée personnelle : propriétaire réduit à {@link ProprietairePublicDTO},
 * aucune information sur le locataire, aucun document (contrats, quittances, documents).
 */
public record BienPublicDTO(

        Long          id,

        // ─── Informations générales ───────────────────────────────────────────
        String        titre,
        TypeBien      typeBien,
        String        description,

        // ─── Adresse ──────────────────────────────────────────────────────────
        String        adresse,
        String        ville,
        String        codePostal,
        String        pays,
        Double        latitude,
        Double        longitude,

        // ─── Caractéristiques physiques ───────────────────────────────────────
        Double        surfaceHabitable,
        Integer       nombrePieces,
        Integer       nombreChambres,
        Integer       etage,
        Boolean       ascenseur,
        Integer       anneeConstruction,
        ModeChauffage modeChauffage,

        // ─── Diagnostic énergétique ───────────────────────────────────────────
        ClasseEnergie classeEnergie,
        ClasseGes     classeGes,

        // ─── Conditions de location ───────────────────────────────────────────
        Double        loyerMensuel,
        Double        chargesMensuelles,
        Double        depotGarantie,
        Boolean       meuble,
        Boolean       colocation,
        LocalDate     disponibleDe,

        // ─── Propriétaire (vue publique) ──────────────────────────────────────
        ProprietairePublicDTO proprietaire,

        // ─── Médias ───────────────────────────────────────────────────────────
        List<String>  images,
        List<String>  pois,

        // ─── Audit ────────────────────────────────────────────────────────────
        LocalDateTime createdAt,
        LocalDateTime updatedAt

) {}