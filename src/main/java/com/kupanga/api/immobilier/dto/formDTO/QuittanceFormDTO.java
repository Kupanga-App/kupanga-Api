package com.kupanga.api.immobilier.dto.formDTO;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class QuittanceFormDTO {

    @NotNull
    private Long bienId;

    @NotNull
    private String emailLocataire;

    // Contrat optionnel — si renseigné, loyer et charges sont récupérés automatiquement
    private Long contratId;

    @NotEmpty
    private String mois;

    @NotNull
    @Min(2000)
    private Integer annee;

    // Si contratId non fourni, loyer et charges sont obligatoires (plafonds C5 selon la devise du bien)
    @DecimalMin(value = "0.01", message = "Le loyer doit être supérieur à 0")
    @DecimalMax(value = "9999999999.99", message = "Montant trop élevé") // technique (NUMERIC(12,2)), avant les plafonds C5
    @Digits(integer = 10, fraction = 2, message = "Format invalide (ex: 850.00)")
    private BigDecimal loyerMensuel;
    @DecimalMin(value = "0.0", message = "Les charges ne peuvent pas être négatives")
    @DecimalMax(value = "9999999999.99", message = "Montant trop élevé") // technique (NUMERIC(12,2)), avant les plafonds C5
    @Digits(integer = 10, fraction = 2, message = "Format invalide (ex: 50.00)")
    private BigDecimal chargesMensuelles;

    private LocalDate datePaiement;

    @NotNull
    private LocalDate dateEcheance;
}