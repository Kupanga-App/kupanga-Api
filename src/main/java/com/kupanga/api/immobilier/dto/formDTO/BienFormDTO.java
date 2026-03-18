package com.kupanga.api.immobilier.dto.formDTO;

import com.kupanga.api.immobilier.entity.ClasseEnergie;
import com.kupanga.api.immobilier.entity.ClasseGes;
import com.kupanga.api.immobilier.entity.ModeChauffage;
import com.kupanga.api.immobilier.entity.TypeBien;
import com.kupanga.api.immobilier.validation.NoUrl;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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
    @Pattern(regexp = "^[\\p{L}0-9 ,.'\\-]+$", message = "Caractères non autorisés")
    @NoUrl
    private String adresse;

    @NotBlank(message = "La ville est obligatoire")
    @Size(min = 2, max = 100, message = "Entre 2 et 100 caractères")
    @Pattern(regexp = "^[\\p{L} \\-']+$", message = "Lettres, espaces, tirets ou apostrophes uniquement")
    @NoUrl
    private String ville;

    @NotBlank(message = "Le code postal est obligatoire")
    @Pattern(regexp = "^[0-9A-Z\\- ]{3,10}$", message = "Code postal invalide (ex: 44000)")
    private String codePostal;

    @NotBlank(message = "Le pays est obligatoire")
    @Size(min = 2, max = 100)
    @Pattern(regexp = "^[\\p{L} \\-']+$", message = "Caractères non autorisés")
    @NoUrl
    private String pays;

    // ─── Caractéristiques physiques ───────────────────────────────────────────

    @NotNull(message = "La surface habitable est obligatoire")
    @DecimalMin(value = "9.0",    message = "La surface minimale est de 9 m²")
    @DecimalMax(value = "10000.0", message = "La surface semble invalide")
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
    @DecimalMin(value = "1.0",      message = "Le loyer doit être supérieur à 0")
    @DecimalMax(value = "100000.0", message = "Le loyer semble invalide")
    @Digits(integer = 8, fraction = 2, message = "Format invalide (ex: 850.00)")
    private Double loyerMensuel;

    @NotNull(message = "Les charges mensuelles sont obligatoires")
    @DecimalMin(value = "0.0",     message = "Les charges ne peuvent pas être négatives")
    @DecimalMax(value = "10000.0", message = "Les charges semblent invalides")
    @Digits(integer = 6, fraction = 2, message = "Format invalide (ex: 50.00)")
    private Double chargesMensuelles;

    @NotNull(message = "Le dépôt de garantie est obligatoire")
    @DecimalMin(value = "0.0",      message = "Le dépôt ne peut pas être négatif")
    @DecimalMax(value = "100000.0", message = "Le dépôt semble invalide")
    @Digits(integer = 8, fraction = 2, message = "Format invalide (ex: 1700.00)")
    private Double depotGarantie;

    @NotNull(message = "Veuillez préciser si le bien est meublé ou non")
    private Boolean meuble;

    @NotNull(message = "Veuillez préciser si la colocation est autorisée")
    private Boolean colocation;

    @NotNull(message = "La date de disponibilité est obligatoire")
    @FutureOrPresent(message = "La date de disponibilité ne peut pas être dans le passé")
    private LocalDate disponibleDe;
}