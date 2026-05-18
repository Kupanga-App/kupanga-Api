package com.kupanga.api.exception.business;

import org.springframework.http.HttpStatus;

/**
 * Levée quand aucun utilisateur ne correspond à l'email fourni.
 */
public class UserNotFoundException extends BusinessException {

    /**
     * @param email l'adresse email pour laquelle aucun utilisateur n'a été trouvé
     */
    public UserNotFoundException(String email) {
        super("Aucun utilisateur trouvé pour l'email : " + email, HttpStatus.NOT_FOUND);
    }
}
