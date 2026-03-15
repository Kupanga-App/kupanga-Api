package com.kupanga.api.immobilier.dto.formDTO;

import com.kupanga.api.immobilier.validation.DateBailValide;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;

@DateBailValide
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContratFormDTO {

    // ─── Bien ─────────────────────────────────────────────────────────────────

    @NotNull(message = "L'identifiant du bien est obligatoire")
    @Positive(message = "L'identifiant du bien doit être un nombre positif")
    private Long bienId;

    // ─── Locataire ────────────────────────────────────────────────────────────

    @NotBlank(message = "L'email du locataire est obligatoire")
    @Email(message = "L'email du locataire est invalide")
    @Size(max = 150, message = "L'email ne peut pas dépasser 150 caractères")
    private String emailLocataire;

    // ─── Dates du bail ────────────────────────────────────────────────────────

    @NotNull(message = "La date de début du bail est obligatoire")
    @FutureOrPresent(message = "La date de début ne peut pas être dans le passé")
    private LocalDate dateDebut;

    @Future(message = "La date de fin doit être dans le futur")
    private LocalDate dateFin;  // null si bail illimité

    @NotNull(message = "La durée du bail est obligatoire")
    @Min(value = 1,  message = "La durée minimale du bail est de 1 mois")
    @Max(value = 120, message = "La durée maximale du bail est de 120 mois (10 ans)")
    private Integer dureeBailMois;

    // ─── Conditions financières ───────────────────────────────────────────────

    @NotNull(message = "Le loyer mensuel est obligatoire")
    @DecimalMin(value = "1.0",      message = "Le loyer doit être supérieur à 0")
    @DecimalMax(value = "100000.0", message = "Le loyer semble invalide")
    @Digits(integer = 8, fraction = 2, message = "Format invalide (ex: 850.00)")
    private Double loyerMensuel;

    @NotNull(message = "Les charges mensuelles sont obligatoires")
    @DecimalMin(value = "0.0",      message = "Les charges ne peuvent pas être négatives")
    @DecimalMax(value = "10000.0",  message = "Les charges semblent invalides")
    @Digits(integer = 6, fraction = 2, message = "Format invalide (ex: 50.00)")
    private Double chargesMensuelles;

    @NotNull(message = "Le dépôt de garantie est obligatoire")
    @DecimalMin(value = "0.0",      message = "Le dépôt de garantie ne peut pas être négatif")
    @DecimalMax(value = "100000.0", message = "Le dépôt de garantie semble invalide")
    @Digits(integer = 8, fraction = 2, message = "Format invalide (ex: 1700.00)")
    private Double depotGarantie;
}