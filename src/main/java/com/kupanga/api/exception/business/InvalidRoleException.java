package com.kupanga.api.exception.business;


import org.springframework.http.HttpStatus;

/**
 * Exception levée lorsqu'un rôle métier invalide est détecté.
 *
 * <p>
 * Cette exception est utilisée lorsque l'application rencontre un rôle
 * qui ne correspond pas aux règles métier attendues.
 * </p>
 *
 * <p>
 * Elle étend {@link BusinessException} et retourne un {@link HttpStatus#BAD_REQUEST}.
 * </p>
 *
 * <p>
 * Exemple d'utilisation :
 * <pre>
 * if (role == null ) {
 *     throw new InvalidRoleException();
 * }
 * if (!validRoles.contains(role)) {
 *      throw new InvalidRoleException(message);
 * }
 * </pre>
 * </p>
 */
public class InvalidRoleException extends BusinessException {

    /**
     * Construit une exception pour un rôle invalide donné.
     */
    public InvalidRoleException() {
        super("Rôle métier invalide ", HttpStatus.BAD_REQUEST);
    }

    /**
     * Construit une exception pour un rôle invalide avec un message personnalisé.
     *
     * @param message message métier décrivant l'erreur
     */
    public InvalidRoleException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }
}


