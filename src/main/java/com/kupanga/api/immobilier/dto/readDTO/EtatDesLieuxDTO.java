package com.kupanga.api.immobilier.dto.readDTO;

import com.kupanga.api.immobilier.entity.StatutEdl;
import com.kupanga.api.immobilier.entity.TypeEtat;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Set;

@Getter
@Setter
public class EtatDesLieuxDTO {

    private Long id;
    private TypeEtat type;
    private StatutEdl statut;
    private LocalDate dateRealisation;
    private LocalTime heureRealisation;
    private String observations;
    private String urlPdf;

    // ─── Signatures ───────────────────────────────────────────────────────────
    private String signatureProprietaire;
    private LocalDateTime dateSignatureProprietaire;
    private String signatureLocataire;
    private LocalDateTime dateSignatureLocataire;

    // ─── Parties ──────────────────────────────────────────────────────────────
    private String nomProprietaire;
    private String emailProprietaire;
    private String nomLocataire;
    private String emailLocataire;

    // ─── Bien ─────────────────────────────────────────────────────────────────
    private String adresseBien;
    private String typeBien;

    // ─── Collections ──────────────────────────────────────────────────────────
    private Set<PieceEdlDTO> pieces;
    private Set<CompteurReleveDTO> compteurs;
    private Set<CleRemiseDTO> cles;

    // ── Sous-DTOs ─────────────────────────────────────────────────────────────

    @Getter @Setter
    public static class PieceEdlDTO {
        private Long id;
        private String nomPiece;
        private Integer ordre;
        private String observations;
        private Set<ElementEdlDTO> elements;
    }

    @Getter @Setter
    public static class ElementEdlDTO {
        private Long id;
        private String typeElement;
        private String etatElement;
        private String description;
        private String observation;
    }

    @Getter @Setter
    public static class CompteurReleveDTO {
        private Long id;
        private String typeCompteur;
        private String numeroCompteur;
        private Double index;
        private String unite;
    }

    @Getter @Setter
    public static class CleRemiseDTO {
        private Long id;
        private String typeCle;
        private Integer quantite;
    }
}