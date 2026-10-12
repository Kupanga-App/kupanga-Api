package com.kupanga.api.immobilier.dto.formDTO;

import com.kupanga.api.juridiction.Devise;
import com.kupanga.api.immobilier.entity.ClasseEnergie;
import com.kupanga.api.immobilier.entity.Equipement;
import com.kupanga.api.immobilier.entity.ClasseGes;
import com.kupanga.api.immobilier.entity.ModeChauffage;
import com.kupanga.api.immobilier.entity.TypeBien;
import com.kupanga.api.immobilier.validation.NoUrl;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

/**
 * DTO de mise à jour partielle d'un bien (PATCH).
 *
 * Conventions :
 *  - Tout champ absent du JSON (null) = valeur inchangée en base.
 *  - Pour vider la description, envoyer explicitement "description": "".
 *  - Les champs d'adresse (adresse, ville, codePostal, pays) ne sont pas modifiables.
 *  - C2 : latitude + longitude (ensemble) déplacent le repère du bien ; les POI sont recalculés.
 *  - Les validations de format s'appliquent uniquement si la valeur est fournie.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BienUpdateDTO {

    // ─── Informations générales ───────────────────────────────────────────────

    @Size(min = 3, max = 150, message = "Entre 3 et 150 caractères")
    @Pattern(regexp = "^[\\p{L}0-9 ,.'\"\\-()]+$", message = "Caractères non autorisés dans le titre")
    @NoUrl
    private String titre;

    @NotNull(message = "Le type de bien est obligatoire")
    private TypeBien typeBien;

    // null = inchangé · "" (chaîne vide) = effacement explicite
    @Size(max = 1000, message = "1000 caractères maximum")
    @NoUrl
    private String description;

    // ─── Caractéristiques physiques ───────────────────────────────────────────

    @DecimalMin(value = "9.0",     message = "La surface minimale est de 9 m²")
    @DecimalMax(value = "10000.0", message = "La surface semble invalide")
    private Double surfaceHabitable;

    @Min(value = 1,  message = "Le nombre de pièces minimum est 1")
    @Max(value = 50, message = "Le nombre de pièces semble invalide")
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

    // C5 : plafond selon la devise du bien (JuridictionRegistry.verifierMontants)
    @DecimalMin(value = "0.01",     message = "Le loyer doit être supérieur à 0")
    @DecimalMax(value = "9999999999.99", message = "Montant trop élevé") // technique (NUMERIC(12,2)), avant les plafonds C5
    @Digits(integer = 10, fraction = 2, message = "Format invalide (ex: 850.00)")
    private BigDecimal loyerMensuel;

    @DecimalMin(value = "0.0",     message = "Les charges ne peuvent pas être négatives")
    @DecimalMax(value = "9999999999.99", message = "Montant trop élevé") // technique (NUMERIC(12,2)), avant les plafonds C5
    @Digits(integer = 10, fraction = 2, message = "Format invalide (ex: 50.00)")
    private BigDecimal chargesMensuelles;

    @DecimalMin(value = "0.0",      message = "Le dépôt ne peut pas être négatif")
    @DecimalMax(value = "9999999999.99", message = "Montant trop élevé") // technique (NUMERIC(12,2)), avant les plafonds C5
    @Digits(integer = 10, fraction = 2, message = "Format invalide (ex: 1700.00)")
    private BigDecimal depotGarantie;

    /** J3 : nouvelle devise (parmi celles du pays) ; les documents existants gardent la leur. */
    private Devise devise;

    private Boolean meuble;
    private Boolean colocation;

    // Pas de @FutureOrPresent : une date existante déjà passée resterait valide
    private LocalDate disponibleDe;

    // ─── C2 : repère sur la carte ou GPS du téléphone (déplacement du repère, les deux ensemble) ───

    @DecimalMin(value = "-90.0", message = "La latitude doit être comprise entre -90 et 90")
    @DecimalMax(value = "90.0", message = "La latitude doit être comprise entre -90 et 90")
    private Double latitude;

    @DecimalMin(value = "-180.0", message = "La longitude doit être comprise entre -180 et 180")
    @DecimalMax(value = "180.0", message = "La longitude doit être comprise entre -180 et 180")
    private Double longitude;

    @JsonIgnore
    @AssertTrue(message = "La latitude et la longitude du repère doivent être fournies ensemble")
    public boolean isRepereComplet() {
        return (latitude == null) == (longitude == null);
    }

    // ─── C11 : équipements (liste complète remplacée si fournie) et heures de courant ─────

    @Size(max = 20, message = "20 équipements au maximum")
    private Set<@NotNull(message = "Équipement invalide") Equipement> equipements;

    @Min(value = 0,  message = "Entre 0 et 24 heures")
    @Max(value = 24, message = "Entre 0 et 24 heures")
    private Integer heuresElectriciteParJour;

    // ─── C12 : garantie en mois de loyer et loyers payés d'avance (RDC) ─────

    @Min(value = 0,  message = "Entre 0 et 24 mois")
    @Max(value = 24, message = "Entre 0 et 24 mois")
    private Integer garantieMois;

    @Min(value = 0,  message = "Entre 0 et 24 mois")
    @Max(value = 24, message = "Entre 0 et 24 mois")
    private Integer avanceMois;
}
