package com.kupanga.api.utilisateur.service;

import com.kupanga.api.exception.business.InvalidRoleException;
import com.kupanga.api.exception.business.UserAlreadyExistsException;
import com.kupanga.api.exception.business.UserNotFoundException;
import com.kupanga.api.login.dto.AuthResponseDTO;
import com.kupanga.api.login.dto.LoginDTO;
import com.kupanga.api.utilisateur.entity.Role;
import com.kupanga.api.utilisateur.entity.Utilisateur;

/**
 * Service métier responsable de la gestion des utilisateurs.
 *
 * <p>
 * Cette interface définit les règles métier liées aux utilisateurs :
 * récupération, vérification d'existence et persistance.
 * </p>
 *
 * <p>
 * Toute implémentation doit garantir le respect des contraintes métier
 * (unicité de l'email, cohérence des données, etc.).
 * </p>
 */
public interface UtilisateurService {

    /**
     * Récupère un utilisateur à partir de son adresse email.
     *
     * @param email l'adresse email de l'utilisateur recherché
     * @return l'utilisateur correspondant à l'email fourni
     * @throws UserNotFoundException
     *         si aucun utilisateur n'est trouvé pour cet email
     */
    Utilisateur getUtilisateurByEmail(String email) throws UserNotFoundException;

    /**
     * Vérifie si un utilisateur existe déjà pour l'email fourni.
     *
     * <p>
     * Cette méthode est généralement utilisée avant la création
     * d'un nouvel utilisateur afin de garantir l'unicité de l'email.
     * </p>
     *
     * @param email l'adresse email à vérifier
     * @throws UserAlreadyExistsException
     *         si un utilisateur existe déjà avec cet email
     */
    void verifieSiUtilisateurEstPresent(String email) throws UserAlreadyExistsException;

    /**
     * Enregistre un utilisateur en base de données.
     *
     * <p>
     * Cette méthode persiste l'entité {@link Utilisateur}.
     * Les règles métier (validation, unicité, etc.) doivent être
     * appliquées avant l'appel à cette méthode.
     * </p>
     *
     * @param utilisateur l'utilisateur à sauvegarder
     */
    void save(Utilisateur utilisateur);

    /**
     * Vérifie que le rôle fourni correspond à un rôle valide défini dans l'application.
     *
     * @param role le rôle de l'utilisateur
     * @throws InvalidRoleException si le rôle n'est pas reconnu ou invalide
     */
    void verifieSiRoleUtilisateurCorrect(Role role) throws InvalidRoleException;

    /**
     * Vérifie que le rôle fourni correspond au rôle "LOCATAIRE".
     *
     * @param role le rôle de l'utilisateur
     * @throws InvalidRoleException si le rôle n'est pas "ROLE_LOCATAIRE"
     */
    void verifieSiUtilisateurEstLocataire(Role role) throws InvalidRoleException;

    /**
     * Vérifie que le rôle fourni correspond au rôle "PROPRIETAIRE".
     *
     * @param role le rôle de l'utilisateur
     * @throws InvalidRoleException si le rôle n'est pas "ROLE_PROPRIETAIRE"
     */
    void verifieSiUtilisateurEstProprietaire(Role role) throws InvalidRoleException;

    /**
     * Vérifie si le mot de passe fourni lors de la connexion correspond
     * au mot de passe stocké de l'utilisateur.
     *
     * @param passwordLogin Le mot de passe fourni par l'utilisateur lors de la connexion
     * @param passwordUser  Le mot de passe encodé stocké pour l'utilisateur
     * @throws IllegalArgumentException si le mot de passe ne correspond pas
     */
    void isCorrectPassword(String passwordLogin, String passwordUser);

    /**
     * Authentifie un utilisateur avec ses identifiants de connexion.
     *
     * Cette méthode effectue les opérations suivantes :
     * <ul>
     *     <li>Récupère l'utilisateur à partir de son email</li>
     *     <li>Vérifie que le mot de passe fourni correspond au mot de passe stocké</li>
     *     <li>Génère un JWT et un token de refresh</li>
     *     <li>Retourne les informations d'authentification dans un DTO</li>
     * </ul>
     *
     * @param loginDTO Objet contenant l'email et le mot de passe de l'utilisateur
     * @return AuthResponseDTO contenant le JWT, le refresh token, l'email et le rôle
     * @throws UserNotFoundException si aucun utilisateur n'est trouvé pour l'email fourni
     * @throws IllegalArgumentException si le mot de passe est incorrect
     */
    AuthResponseDTO login(LoginDTO loginDTO);

}
