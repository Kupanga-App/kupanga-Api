package com.kupanga.api.chat.sort;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum ConversationSortEnum {

    ID("id"),
    CREATED_AT("createdAt"),
    LAST_MESSAGE_AT("lastMessageAt");

    private final String fieldName;

    /**
     * Vérifie si la valeur correspond à un nom de champ valide de l'énumération.
     *
     * @param value le nom du champ à vérifier
     * @return true si le champ existe dans l'énumération, false sinon
     */
    public static boolean isValidField(String value) {
        if (value == null || value.isBlank()) return false;
        return Arrays.stream(values())
                .anyMatch(field -> field.getFieldName().equalsIgnoreCase(value));
    }

    /**
     * Résout le nom de champ correspondant à la valeur fournie.
     * Retourne le champ par défaut (ID) si la valeur est nulle, vide ou invalide.
     *
     * @param value le nom du champ à résoudre
     * @return le nom de champ valide ou le champ par défaut
     */
    public static String resolveField(String value) {
        if (value == null || value.isBlank()) return ID.fieldName;
        return Arrays.stream(values())
                .map(ConversationSortEnum::getFieldName)
                .filter(name -> name.equalsIgnoreCase(value))
                .findFirst()
                .orElse(ID.fieldName);
    }
}
