package com.kupanga.api.exception.business;

import org.springframework.http.HttpStatus;

/**
 * Levée lors d'une tentative de création d'un compte avec un email déjà utilisé.
 */
public class UserAlreadyExistsException extends BusinessException {

    /**
     * @param email l'adresse email en conflit
     */
    public UserAlreadyExistsException(String email) {
        super("Un utilisateur existe déjà avec l'email : " + email, HttpStatus.CONFLICT);
    }
}
