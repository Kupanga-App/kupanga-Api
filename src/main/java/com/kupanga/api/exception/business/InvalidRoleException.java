package com.kupanga.api.exception.business;

import org.springframework.http.HttpStatus;

/**
 * Levée quand le rôle de l'utilisateur ne correspond pas au rôle attendu pour l'opération.
 */
public class InvalidRoleException extends BusinessException {

    /** Construit l'exception avec le message par défaut "Rôle métier invalide". */
    public InvalidRoleException() {
        super("Rôle métier invalide ", HttpStatus.BAD_REQUEST);
    }

    /**
     * Construit l'exception avec un message personnalisé.
     *
     * @param message description précise de l'erreur de rôle
     */
    public InvalidRoleException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }
}
