package com.kupanga.api.immobilier.dto.readDTO;

import com.kupanga.api.immobilier.entity.*;
import com.kupanga.api.user.dto.readDTO.UserDTO;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public record BienDTO(

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

        // ─── Parties ──────────────────────────────────────────────────────────
        UserDTO       proprietaire,
        UserDTO       locataire,

        // ─── Documents & médias ───────────────────────────────────────────────
        List<String>  contrats,
        List<String>  quittances,
        List<String>  documents,
        List<String>  images,
        List<String>  pois,

        // ─── Audit ────────────────────────────────────────────────────────────
        LocalDateTime createdAt,
        LocalDateTime updatedAt

) {}