package com.kupanga.api.immobilier.research.sort;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum QuittanceSortEnum {

    ANNEE("annee"),
    DATE_ECHEANCE("dateEcheance"),
    DATE_PAIEMENT("datePaiement"),
    MONTANT_TOTAL("montantTotal"),
    STATUT("statut"),
    CREATED_AT("createdAt");

    private final String fieldName;

    public static boolean isValidField(String value) {
        if (value == null || value.isBlank()) return false;
        return Arrays.stream(values())
                .anyMatch(f -> f.getFieldName().equalsIgnoreCase(value));
    }

    public static String resolveField(String value) {
        if (value == null || value.isBlank()) return ANNEE.fieldName;
        return Arrays.stream(values())
                .map(QuittanceSortEnum::getFieldName)
                .filter(name -> name.equalsIgnoreCase(value))
                .findFirst()
                .orElse(ANNEE.fieldName);
    }
}
