package com.kupanga.api.pagination.sort;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum SortEnum {

    ID("id"),
    URL("url");

    private final String fieldName;

    /**
     * Vérifie si la chaîne passée correspond à l'un des fieldName de l'enum
     *
     * @param value Le nom du champ à vérifier
     * @return true si le champ existe, false sinon
     */
    public static boolean isValidField(String value) {
        if (value == null || value.isBlank()) return false;

        return Arrays.stream(SortEnum.values())
                .anyMatch(field -> field.getFieldName().equalsIgnoreCase(value));
    }

}
