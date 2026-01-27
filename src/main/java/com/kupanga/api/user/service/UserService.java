package com.kupanga.api.user.service;

import com.kupanga.api.exception.business.InvalidRoleException;
import com.kupanga.api.exception.business.UserAlreadyExistsException;
import com.kupanga.api.exception.business.UserNotFoundException;
import com.kupanga.api.user.entity.Role;
import com.kupanga.api.user.entity.User;


public interface UserService {

    /**
     * Récupère un utilisateur à partir de son adresse email.
     *
     * @param email l'adresse email de l'utilisateur recherché
     * @return l'utilisateur correspondant à l'email fourni
     * @throws UserNotFoundException
     *         si aucun utilisateur n'est trouvé pour cet email
     */
    User getUserByEmail(String email) throws UserNotFoundException;

    /**
     * Vérifie si un utilisateur existe déjà pour l'email fourni.
     *
     * @param email l'adresse email à vérifier
     * @throws UserAlreadyExistsException
     *         si un utilisateur existe déjà avec cet email
     */
    void verifyIfUserExistWithEmail(String email) throws UserAlreadyExistsException;

    /**
     * Enregistre un utilisateur en base de données.
     *
     * @param user l'utilisateur à sauvegarder
     */
    void save(User user);

    /**
     * Vérifie que le rôle fourni correspond à un rôle valide défini dans l'application.
     *
     * @param role le rôle de l'utilisateur
     * @throws InvalidRoleException si le rôle n'est pas reconnu ou invalide
     */
    void verifyIfRoleOfUserValid(Role role) throws InvalidRoleException;

    /**
     * Vérifie que le rôle fourni correspond au rôle "LOCATAIRE".
     *
     * @param role le rôle de l'utilisateur
     * @throws InvalidRoleException si le rôle n'est pas "ROLE_LOCATAIRE"
     */
    void verifyIfUserIsTenant(Role role) throws InvalidRoleException;

    /**
     * Vérifie que le rôle fourni correspond au rôle "PROPRIETAIRE".
     *
     * @param role le rôle de l'utilisateur
     * @throws InvalidRoleException si le rôle n'est pas "ROLE_PROPRIETAIRE"
     */
    void verifyIfUserIsOwner(Role role) throws InvalidRoleException;

    /**
     * Vérifie si le mot de passe fourni lors de la connexion correspond
     * au mot de passe stocké de l'utilisateur.
     *
     * @param passwordLogin Le mot de passe fourni par l'utilisateur lors de la connexion
     * @param passwordUser  Le mot de passe encodé stocké pour l'utilisateur
     * @throws IllegalArgumentException si le mot de passe ne correspond pas
     */
    void isCorrectPassword(String passwordLogin, String passwordUser);

}
