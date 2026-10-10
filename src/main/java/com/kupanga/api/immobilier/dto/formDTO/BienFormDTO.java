package com.kupanga.api.immobilier.dto.formDTO;

import com.kupanga.api.juridiction.Devise;
import com.kupanga.api.juridiction.Pays;
import com.kupanga.api.immobilier.entity.ClasseEnergie;
import com.kupanga.api.immobilier.entity.ClasseGes;
import com.kupanga.api.immobilier.entity.ModeChauffage;
import com.kupanga.api.immobilier.entity.TypeBien;
import com.kupanga.api.immobilier.validation.NoUrl;
import com.kupanga.api.immobilier.validation.ValideSelonJuridiction;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
// J4 : champs obligatoires / masqués, types de bien, devise et plafonds selon le pays (JuridictionRegistry)
@ValideSelonJuridiction
public class BienFormDTO {

    // ─── Informations générales ───────────────────────────────────────────────

    @NotBlank(message = "Le titre est obligatoire")
    @Size(min = 3, max = 150, message = "Entre 3 et 150 caractères")
    @Pattern(regexp = "^[\\p{L}0-9 ,.'\"\\-()]+$", message = "Caractères non autorisés")
    @NoUrl
    private String titre;

    @NotNull(message = "Le type de bien est obligatoire")
    private TypeBien typeBien;

    @Size(max = 1000, message = "1000 caractères maximum")
    @NoUrl
    private String description;

    // ─── Adresse ──────────────────────────────────────────────────────────────

    @NotBlank(message = "L'adresse est obligatoire")
    @Size(min = 5, max = 200, message = "Entre 5 et 200 caractères")
    // C3 : accepte aussi °, /, #, & (« N° 12, Av. Kasa-Vubu, Q/Matonge, C/Kalamu »)
    @Pattern(regexp = "^[\\p{L}0-9 ,.'°/#&\\-]+$", message = "Caractères non autorisés")
    @NoUrl
    private String adresse;

    @NotBlank(message = "La ville est obligatoire")
    @Size(min = 2, max = 100, message = "Entre 2 et 100 caractères")
    @Pattern(regexp = "^[\\p{L} \\-']+$", message = "Lettres, espaces, tirets ou apostrophes uniquement")
    @NoUrl
    private String ville;

    /** C1 : obligatoire ou masqué selon le pays (obligatoire en France, sans objet en RDC). */
    @Pattern(regexp = "^([0-9A-Z\\- ]{3,10})?$", message = "Code postal invalide (ex: 44000)")
    private String codePostal;

    // ─── Adresse congolaise (C1) : obligatoire ou masquée selon le pays ──────
    // « N° 12, Av. Kasa-Vubu, Q/Matonge, C/Kalamu », « derrière l'église X, en face de la station Y »

    @Size(max = 100, message = "100 caractères maximum")
    @Pattern(regexp = "^[\\p{L}0-9 .'’/\\-]*$", message = "Caractères non autorisés")
    @NoUrl
    private String commune;

    @Size(max = 100, message = "100 caractères maximum")
    @Pattern(regexp = "^[\\p{L}0-9 .'’/\\-]*$", message = "Caractères non autorisés")
    @NoUrl
    private String quartier;

    @Size(max = 150, message = "150 caractères maximum")
    @Pattern(regexp = "^[\\p{L}0-9 ,.'’°/#&\\-]*$", message = "Caractères non autorisés")
    @NoUrl
    private String avenue;

    @Size(max = 50, message = "50 caractères maximum")
    @Pattern(regexp = "^[\\p{L}0-9 .°/#\\-]*$", message = "Caractères non autorisés")
    private String numeroParcelle;

    @Size(max = 255, message = "255 caractères maximum")
    @Pattern(regexp = "^[\\p{L}0-9 ,.'’°/#&()\\-]*$", message = "Caractères non autorisés")
    @NoUrl
    private String pointDeRepere;

    /** J1 : code ISO (FR, BE, CD, CG) ; une autre valeur est refusée en 400. */
    @NotNull(message = "Le pays est obligatoire")
    private Pays pays;

    // ─── Caractéristiques physiques ───────────────────────────────────────────

    @NotNull(message = "La surface habitable est obligatoire")
    @DecimalMin(value = "9.0",    message = "La surface minimale est de 9 m²")
    private Double surfaceHabitable;

    @NotNull(message = "Le nombre de pièces est obligatoire")
    @Min(value = 1,   message = "Le nombre de pièces minimum est 1")
    @Max(value = 50,  message = "Le nombre de pièces semble invalide")
    private Integer nombrePieces;

    @Min(value = 0,  message = "Le nombre de chambres ne peut pas être négatif")
    @Max(value = 20, message = "Le nombre de chambres semble invalide")
    private Integer nombreChambres;

    @Min(value = 0,   message = "L'étage ne peut pas être négatif")
    @Max(value = 200, message = "L'étage semble invalide")
    private Integer etage;

    private Boolean ascenseur;

    @Min(value = 1800, message = "L'année de construction semble invalide")
    @Max(value = 2100, message = "L'année de construction semble invalide")
    private Integer anneeConstruction;

    private ModeChauffage modeChauffage;

    // ─── Diagnostic énergétique ───────────────────────────────────────────────

    private ClasseEnergie classeEnergie;
    private ClasseGes     classeGes;

    // ─── Conditions de location ───────────────────────────────────────────────

    @NotNull(message = "Le loyer mensuel est obligatoire")
    @DecimalMin(value = "0.01",      message = "Le loyer doit être supérieur à 0")
    @DecimalMax(value = "9999999999.99", message = "Montant trop élevé") // technique (NUMERIC(12,2)), avant les plafonds C5
    @Digits(integer = 10, fraction = 2, message = "Format invalide (ex: 850.00)")
    private BigDecimal loyerMensuel;

    @NotNull(message = "Les charges mensuelles sont obligatoires")
    @DecimalMin(value = "0.0",     message = "Les charges ne peuvent pas être négatives")
    @DecimalMax(value = "9999999999.99", message = "Montant trop élevé") // technique (NUMERIC(12,2)), avant les plafonds C5
    @Digits(integer = 10, fraction = 2, message = "Format invalide (ex: 50.00)")
    private BigDecimal chargesMensuelles;

    @NotNull(message = "Le dépôt de garantie est obligatoire")
    @DecimalMin(value = "0.0",      message = "Le dépôt ne peut pas être négatif")
    @DecimalMax(value = "9999999999.99", message = "Montant trop élevé") // technique (NUMERIC(12,2)), avant les plafonds C5
    @Digits(integer = 10, fraction = 2, message = "Format invalide (ex: 1700.00)")
    private BigDecimal depotGarantie;

    /** J3 : devise des montants ; absente → devise par défaut du pays. Refusée (400) si le pays ne l'accepte pas. */
    private Devise devise;

    @NotNull(message = "Veuillez préciser si le bien est meublé ou non")
    private Boolean meuble;

    @NotNull(message = "Veuillez préciser si la colocation est autorisée")
    private Boolean colocation;

    @NotNull(message = "La date de disponibilité est obligatoire")
    @FutureOrPresent(message = "La date de disponibilité ne peut pas être dans le passé")
    private LocalDate disponibleDe;
}