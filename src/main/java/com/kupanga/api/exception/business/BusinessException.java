package com.kupanga.api.exception.business;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Exception racine pour toutes les erreurs métier de l'application.
 *
 * <p>
 * Toute violation d'une règle métier doit lever une {@code BusinessException}
 * ou l'une de ses sous-classes. Cela permet de centraliser la gestion
 * des erreurs métier et d'utiliser un {@link HttpStatus} approprié
 * pour chaque cas.
 * </p>
 *
 * <p>
 * Exemple :
 * <pre>
 * if (utilisateurRepository.existsByEmail(email)) {
 *     throw new UserAlreadyExistsException(email);
 * }
 * </pre>
 * </p>
 */
@Getter
public abstract class BusinessException extends RuntimeException {

    /**
     * Le code HTTP associé à cette exception.
     *  Retourne le code HTTP associé à cette exception.
     */
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
