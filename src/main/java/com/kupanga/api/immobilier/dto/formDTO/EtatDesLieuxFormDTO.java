package com.kupanga.api.immobilier.dto.formDTO;

import com.kupanga.api.immobilier.entity.TypeEtat;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Getter
@Setter
public class EtatDesLieuxFormDTO {

    @NotNull
    private Long bienId;

    @NotNull
    private String emailLocataire;

    @NotNull
    private TypeEtat type;                  // ENTREE | SORTIE

    @NotNull
    private LocalDate dateRealisation;

    private LocalTime heureRealisation;

    private String observations;

    private List<PieceEdlFormDTO> pieces;
    private List<CompteurReleveFormDTO> compteurs;
    private List<CleRemiseFormDTO> cles;

    // ── Sous-DTOs ─────────────────────────────────────────────────────────────

    @Getter @Setter
    public static class PieceEdlFormDTO {
        private String nomPiece;
        private Integer ordre;
        private String observations;
        private List<ElementEdlFormDTO> elements;
    }

    @Getter @Setter
    public static class ElementEdlFormDTO {
        private String typeElement;     // valeur de l'enum TypeElement
        private String etatElement;     // valeur de l'enum EtatElement
        private String description;
        private String observation;
    }

    @Getter @Setter
    public static class CompteurReleveFormDTO {
        private String typeCompteur;    // valeur de l'enum TypeCompteur
        private String numeroCompteur;
        private Double index;
        private String unite;
    }

    @Getter @Setter
    public static class CleRemiseFormDTO {
        private String typeCle;
        private Integer quantite;
    }
}