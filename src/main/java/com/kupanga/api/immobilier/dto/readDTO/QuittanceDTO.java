package com.kupanga.api.immobilier.dto.readDTO;

import com.kupanga.api.immobilier.entity.StatutQuittance;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
public class QuittanceDTO {

    private Long   id;
    private Integer mois;
    private Integer annee;
    private String  moisLabel;          // "Mars 2026" — calculé côté service

    // ─── Financier ────────────────────────────────────────────────────────────
    private Double loyerMensuel;
    private Double chargesMensuelles;
    private Double montantTotal;

    // ─── Dates ────────────────────────────────────────────────────────────────
    private LocalDate dateEcheance;
    private LocalDate datePaiement;

    // ─── Statut ───────────────────────────────────────────────────────────────
    private StatutQuittance statut;

    // ─── PDF ──────────────────────────────────────────────────────────────────
    private String urlPdf;

    // ─── Parties ──────────────────────────────────────────────────────────────
    private String nomProprietaire;
    private String emailProprietaire;
    private String nomLocataire;
    private String emailLocataire;

    // ─── Bien ─────────────────────────────────────────────────────────────────
    private String adresseBien;
    private String typeBien;
    private Double surfaceHabitable;

    // ─── Audit ────────────────────────────────────────────────────────────────
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}