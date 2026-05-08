package com.kupanga.api.immobilier.research.sort;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum EtatDesLieuxSortEnum {

    DATE_REALISATION("dateRealisation"),
    TYPE("type"),
    STATUT("statut"),
    CREATED_AT("createdAt");

    private final String fieldName;

    public static boolean isValidField(String value) {
        if (value == null || value.isBlank()) return false;
        return Arrays.stream(values())
                .anyMatch(f -> f.getFieldName().equalsIgnoreCase(value));
    }

    public static String resolveField(String value) {
        if (value == null || value.isBlank()) return DATE_REALISATION.fieldName;
        return Arrays.stream(values())
                .map(EtatDesLieuxSortEnum::getFieldName)
                .filter(name -> name.equalsIgnoreCase(value))
                .findFirst()
                .orElse(DATE_REALISATION.fieldName);
    }
}
