package com.kupanga.api.immobilier.dto.readDTO;

import com.kupanga.api.juridiction.Pays;
import com.kupanga.api.juridiction.Devise;
import com.kupanga.api.immobilier.entity.StatutQuittance;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
public class QuittanceDTO {

    private Long   id;
    private String mois;
    private Integer annee;
    private String  moisLabel;          // "Mars 2026" — calculé côté service

    // ─── Financier ────────────────────────────────────────────────────────────
    private BigDecimal loyerMensuel;
    private BigDecimal chargesMensuelles;
    private BigDecimal montantTotal;
    // J3 : juridiction figée sur la quittance
    private Pays    pays;
    private Devise  devise;
    private String  modeleVersion;

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