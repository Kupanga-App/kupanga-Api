package com.kupanga.api.exception.business;

import org.springframework.http.HttpStatus;

/**
 * Exception levée lorsqu'un utilisateur avec un email donné
 * existe déjà en base de données.
 *
 * <p>
 * Cette exception est utilisée pour empêcher la création
 * d'un doublon utilisateur et respecter les règles métier.
 * </p>
 *
 * <p>
 * Elle étend {@link BusinessException} et retourne un {@link HttpStatus#CONFLICT}.
 * </p>
 *
 * <p>
 * Exemple d'utilisation :
 * <pre>
 * if (utilisateurRepository.existsByEmail(email)) {
 *     throw new UserAlreadyExistsException(email);
 * }
 * </pre>
 * </p>
 */
public class UserAlreadyExistsException extends BusinessException {

    /**
     * Construit une exception pour un email déjà existant.
     *
     * @param email l'email de l'utilisateur déjà présent en base
     */
    public UserAlreadyExistsException(String email) {

        super("Un utilisateur existe déjà avec l'email : " + email, HttpStatus.CONFLICT);
    }
}
