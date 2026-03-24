package com.kupanga.api.immobilier.dto.formDTO;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

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

    // Si contratId non fourni, loyer et charges sont obligatoires
    private Double loyerMensuel;
    private Double chargesMensuelles;

    private LocalDate datePaiement;

    @NotNull
    private LocalDate dateEcheance;
}