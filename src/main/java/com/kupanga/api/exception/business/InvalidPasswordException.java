package com.kupanga.api.exception.business;

import org.springframework.http.HttpStatus;

/**
 * Levée quand les identifiants de connexion sont invalides (e-mail inconnu ou mot de passe incorrect).
 * Le message est volontairement le même dans les deux cas pour ne pas révéler quels comptes existent.
 */
public class InvalidPasswordException extends BusinessException {

    /** Construit l'exception avec le message générique "E-mail ou mot de passe incorrect". */
    public InvalidPasswordException() {
        super("E-mail ou mot de passe incorrect", HttpStatus.UNAUTHORIZED);
    }
}
