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
 * if (role == null || !validRoles.contains(role)) {
 *     throw new InvalidRoleException(role);
 * }
 * </pre>
 * </p>
 */
public class InvalidRoleException extends BusinessException {

    /**
     * Construit une exception pour un rôle invalide donné.
     *
     * @param role le rôle qui est invalide ou non reconnu
     */
    public InvalidRoleException(String role) {
        super("Rôle métier invalide : " + role, HttpStatus.BAD_REQUEST);
    }
}


