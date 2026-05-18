package com.kupanga.api.exception.business;

import org.springframework.http.HttpStatus;

/**
 * Levée quand le mot de passe fourni ne correspond pas au mot de passe enregistré.
 */
public class InvalidPasswordException extends BusinessException {

    /** Construit l'exception avec le message par défaut "Mot de passe invalide". */
    public InvalidPasswordException() {
        super("Mot de passe invalide", HttpStatus.UNAUTHORIZED);
    }
}
