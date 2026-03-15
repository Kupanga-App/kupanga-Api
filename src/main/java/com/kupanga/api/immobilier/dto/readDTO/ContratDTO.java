package com.kupanga.api.immobilier.dto.readDTO;

import com.kupanga.api.immobilier.entity.StatutContrat;
import com.kupanga.api.user.dto.readDTO.UserDTO;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ContratDTO(

        Long            id,

        // ─── Bien ─────────────────────────────────────────────────────────────
        Long            bienId,
        String          adresseBien,

        // ─── Parties ──────────────────────────────────────────────────────────
        UserDTO         proprietaire,
        UserDTO         locataire,

        // ─── Conditions financières ───────────────────────────────────────────
        Double          loyerMensuel,
        Double          chargesMensuelles,
        Double          depotGarantie,

        // ─── Dates ────────────────────────────────────────────────────────────
        LocalDate       dateDebut,
        LocalDate       dateFin,
        Integer         dureeBailMois,

        // ─── Signatures ───────────────────────────────────────────────────────
        Boolean         proprietaireASigné,
        Boolean         locataireASigné,
        LocalDateTime   dateSignatureProprietaire,
        LocalDateTime   dateSignatureLocataire,

        // ─── PDF ──────────────────────────────────────────────────────────────
        String          urlPdf,

        // ─── Statut ───────────────────────────────────────────────────────────
        StatutContrat   statut,

        // ─── Audit ────────────────────────────────────────────────────────────
        LocalDateTime   createdAt,
        LocalDateTime   updatedAt
) {}