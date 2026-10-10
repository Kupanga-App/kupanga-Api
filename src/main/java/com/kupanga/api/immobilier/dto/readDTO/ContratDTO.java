package com.kupanga.api.immobilier.dto.readDTO;

import com.kupanga.api.juridiction.Pays;
import com.kupanga.api.juridiction.Devise;
import com.kupanga.api.immobilier.entity.StatutContrat;
import com.kupanga.api.user.dto.readDTO.UserDTO;

import java.math.BigDecimal;
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
        BigDecimal      loyerMensuel,
        BigDecimal      chargesMensuelles,
        BigDecimal      depotGarantie,
        // J3 : juridiction figée sur le contrat
        Pays            pays,
        Devise          devise,
        String          modeleVersion,

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