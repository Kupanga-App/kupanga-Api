package com.kupanga.api.exception.business;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Exception racine pour toutes les erreurs métier de l'application.
 * Toute violation d'une règle métier doit lever une sous-classe de cette exception
 * en précisant le statut HTTP approprié.
 */
@Getter
public abstract class BusinessException extends RuntimeException {

    /** Code HTTP associé à cette exception. */
    private final HttpStatus status;

    /**
     * Construit une exception métier avec un message et un statut HTTP.
     *
     * @param message le message décrivant l'erreur métier
     * @param status  le code HTTP à renvoyer
     */
    protected BusinessException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

}
