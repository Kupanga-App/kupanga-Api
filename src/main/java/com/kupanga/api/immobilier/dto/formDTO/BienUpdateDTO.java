package com.kupanga.api.immobilier.dto.formDTO;

import com.kupanga.api.immobilier.entity.ClasseEnergie;
import com.kupanga.api.immobilier.entity.ClasseGes;
import com.kupanga.api.immobilier.entity.ModeChauffage;
import com.kupanga.api.immobilier.entity.TypeBien;
import com.kupanga.api.immobilier.validation.NoUrl;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;

/**
 * DTO de mise à jour partielle d'un bien (PATCH).
 *
 * Conventions :
 *  - Tout champ absent du JSON (null) = valeur inchangée en base.
 *  - Pour vider la description, envoyer explicitement "description": "".
 *  - Les champs d'adresse (adresse, ville, codePostal, pays) et la localisation
 *    ne sont pas modifiables via cet endpoint (géocodage géré séparément).
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

    @DecimalMin(value = "1.0",      message = "Le loyer doit être supérieur à 0")
    @DecimalMax(value = "100000.0", message = "Le loyer semble invalide")
    @Digits(integer = 8, fraction = 2, message = "Format invalide (ex: 850.00)")
    private Double loyerMensuel;

    @DecimalMin(value = "0.0",     message = "Les charges ne peuvent pas être négatives")
    @DecimalMax(value = "10000.0", message = "Les charges semblent invalides")
    @Digits(integer = 6, fraction = 2, message = "Format invalide (ex: 50.00)")
    private Double chargesMensuelles;

    @DecimalMin(value = "0.0",      message = "Le dépôt ne peut pas être négatif")
    @DecimalMax(value = "100000.0", message = "Le dépôt semble invalide")
    @Digits(integer = 8, fraction = 2, message = "Format invalide (ex: 1700.00)")
    private Double depotGarantie;

    private Boolean meuble;
    private Boolean colocation;

    // Pas de @FutureOrPresent : une date existante déjà passée resterait valide
    private LocalDate disponibleDe;
}
