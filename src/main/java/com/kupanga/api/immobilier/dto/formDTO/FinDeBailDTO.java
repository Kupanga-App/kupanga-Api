package com.kupanga.api.immobilier.dto.formDTO;

import com.kupanga.api.immobilier.validation.NoUrl;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Fin de bail enregistrée par le propriétaire (2026-11-05).
 *
 * @param depotRestitue montant du dépôt de garantie rendu au locataire (0 à dépôt du bail, devise du bail)
 * @param motifRetenue  obligatoire si le dépôt n'est pas rendu en entier (réparations, loyers impayés…)
 */
public record FinDeBailDTO(
        @NotNull(message = "Le montant du dépôt restitué est obligatoire (0 si rien n'est rendu)")
        @DecimalMin(value = "0.00", message = "Le montant restitué ne peut pas être négatif")
        @DecimalMax(value = "9999999999.99", message = "Montant trop élevé")
        BigDecimal depotRestitue,

        @Size(max = 500, message = "Le motif ne peut pas dépasser 500 caractères")
        @NoUrl
        String motifRetenue
) {
}
