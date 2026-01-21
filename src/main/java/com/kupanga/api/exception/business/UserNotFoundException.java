package com.kupanga.api.exception.business;

import org.springframework.http.HttpStatus;

/**
 * Exception levée lorsqu'un utilisateur ne peut pas être trouvé
 * dans le système.
 *
 * <p>
 * Cette exception est typiquement utilisée dans les cas suivants :
 * <ul>
 *     <li>Recherche d'un utilisateur par email inexistante</li>
 *     <li>Accès à une ressource utilisateur qui n'existe pas</li>
 * </ul>
 *
 * <p>
 * Elle correspond à une erreur HTTP {@link HttpStatus#NOT_FOUND} (404).
 */
public class UserNotFoundException extends BusinessException {

    /**
     * Construit une {@code UserNotFoundException} pour un email donné.
     *
     * @param email l'email de l'utilisateur introuvable
     */
    public UserNotFoundException(String email) {
        super("Aucun utilisateur trouvé pour l'email : " + email, HttpStatus.NOT_FOUND);
    }
}

