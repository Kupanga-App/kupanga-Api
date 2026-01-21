package com.kupanga.api.login.service;

import com.kupanga.api.exception.business.UserAlreadyExistsException;
import com.kupanga.api.utilisateur.dto.readDTO.UtilisateurDTO;

/**
 * Service métier responsable de la création des comptes utilisateurs.
 *
 * <p>
 * Cette interface encapsule le processus de création d'un utilisateur,
 * incluant les règles métier telles que :
 * </p>
 * <ul>
 *   <li>La vérification de l'unicité de l'email</li>
 *   <li>La validation du rôle métier</li>
 *   <li>la création et la persistance de l'utilisateur</li>
 * </ul>
 *
 * <p>
 * Le service retourne une représentation DTO de l'utilisateur créé,
 * destinée à être exposée à l'extérieur de l'application.
 * </p>
 */
public interface CreationCompteService {

    /**
     * Crée un nouveau compte utilisateur.
     *
     * <p>
     * Avant la création, les règles métier suivantes sont appliquées :
     * </p>
     * <ul>
     *   <li>Vérification que l'email n'est pas déjà utilisé</li>
     *   <li>Validation du rôle utilisateur fourni</li>
     * </ul>
     *
     * @param email l'adresse email utilisée comme identifiant de connexion
     * @param role  le rôle métier de l'utilisateur à créer
     *              (ex : PROPRIETAIRE, LOCATAIRE, ADMIN)
     * @return un {@link UtilisateurDTO} représentant l'utilisateur créé
     * @throws UserAlreadyExistsException
     *         si un utilisateur existe déjà avec l'email fourni
     */
    UtilisateurDTO creationUtilisateur(String email, String role)
            throws UserAlreadyExistsException;
}
