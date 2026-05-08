package com.kupanga.api.immobilier.research.sort;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum ContratSortEnum {

    DATE_DEBUT("dateDebut"),
    DATE_FIN("dateFin"),
    DUREE_BAIL("dureeBailMois"),
    LOYER_MENSUEL("loyerMensuel"),
    STATUT("statut"),
    CREATED_AT("createdAt");

    private final String fieldName;

    public static boolean isValidField(String value) {
        if (value == null || value.isBlank()) return false;
        return Arrays.stream(values())
                .anyMatch(f -> f.getFieldName().equalsIgnoreCase(value));
    }

    public static String resolveField(String value) {
        if (value == null || value.isBlank()) return DATE_DEBUT.fieldName;
        return Arrays.stream(values())
                .map(ContratSortEnum::getFieldName)
                .filter(name -> name.equalsIgnoreCase(value))
                .findFirst()
                .orElse(DATE_DEBUT.fieldName);
    }
}
