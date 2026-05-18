package com.kupanga.api.exception.business;

import org.springframework.http.HttpStatus;

/**
 * Levée quand un token (réinitialisation de mot de passe, signature, etc.) est expiré ou invalide.
 */
public class TokenExpiredException extends BusinessException {

    /** Construit l'exception avec le message par défaut "Token expiré". */
    public TokenExpiredException() {
        super("Token expiré", HttpStatus.BAD_REQUEST);
    }
}
