package com.kupanga.api.exception.business;

import org.springframework.http.HttpStatus;

/**
 * Exception métier générique de l'application Kupanga.
 * Utilisée pour toute règle métier ne justifiant pas une exception dédiée.
 */
public class KupangaBusinessException extends BusinessException {

    /**
     * @param message description de l'erreur métier
     * @param status  code HTTP à renvoyer
     */
    public KupangaBusinessException(String message, HttpStatus status) {
        super(message, status);
    }
}
