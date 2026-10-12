package com.kupanga.api.immobilier.dto.formDTO;

import com.kupanga.api.immobilier.entity.EtatElement;
import com.kupanga.api.immobilier.entity.TypeCompteur;
import com.kupanga.api.immobilier.entity.TypeElement;
import com.kupanga.api.immobilier.entity.TypeEtat;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Formulaire d'état des lieux. B8 : enums typés (valeur inconnue → 400 au lieu d'un {@code valueOf} en 500) ;
 * listes et textes bornés (le PDF est généré à partir de tout le contenu, les colonnes courtes font 255 caractères).
 */
@Getter
@Setter
public class EtatDesLieuxFormDTO {

    @NotNull
    private Long bienId;

    @NotNull
    @Email
    @Size(max = 255)
    private String emailLocataire;

    @NotNull
    private TypeEtat type;                  // ENTREE | SORTIE

    @NotNull
    private LocalDate dateRealisation;

    private LocalTime heureRealisation;

    @Size(max = 2000)
    private String observations;

    @Valid
    @Size(max = 50, message = "50 pièces au maximum")
    private List<@NotNull PieceEdlFormDTO> pieces;

    @Valid
    @Size(max = 10, message = "10 compteurs au maximum")
    private List<@NotNull CompteurReleveFormDTO> compteurs;

    @Valid
    @Size(max = 20, message = "20 types de clés au maximum")
    private List<@NotNull CleRemiseFormDTO> cles;

    // ── Sous-DTOs ─────────────────────────────────────────────────────────────

    @Getter @Setter
    public static class PieceEdlFormDTO {
        @NotBlank
        @Size(max = 100)
        private String nomPiece;
        @Min(0) @Max(1000)
        private Integer ordre;
        @Size(max = 2000)
        private String observations;
        @Valid
        @Size(max = 50, message = "50 éléments par pièce au maximum")
        private List<@NotNull ElementEdlFormDTO> elements;
    }

    @Getter @Setter
    public static class ElementEdlFormDTO {
        @NotNull
        private TypeElement typeElement;
        @NotNull
        private EtatElement etatElement;
        @Size(max = 255)
        private String description;
        @Size(max = 1000)
        private String observation;
    }

    @Getter @Setter
    public static class CompteurReleveFormDTO {
        @NotNull
        private TypeCompteur typeCompteur;
        @Size(max = 50)
        private String numeroCompteur;
        @NotNull
        @PositiveOrZero
        private Double index;
        @Size(max = 10)
        private String unite;
    }

    @Getter @Setter
    public static class CleRemiseFormDTO {
        @NotBlank
        @Size(max = 100)
        private String typeCle;
        @NotNull
        @Min(1) @Max(50)
        private Integer quantite;
    }
}
