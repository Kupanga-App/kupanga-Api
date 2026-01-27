package com.kupanga.api.login.service;

import com.kupanga.api.exception.business.UserAlreadyExistsException;
import com.kupanga.api.login.dto.AuthResponseDTO;
import com.kupanga.api.login.dto.LoginDTO;
import com.kupanga.api.utilisateur.dto.readDTO.UserDTO;
import com.kupanga.api.utilisateur.entity.Role;
import jakarta.servlet.http.HttpServletResponse;


public interface LoginService {

    /**
     * Crée un nouveau compte utilisateur.
     *
     * @param loginDTO contient email et mot de passe.
     * @return un {@link UserDTO} représentant l'utilisateur créé
     * @throws UserAlreadyExistsException
     *         si un utilisateur existe déjà avec l'email fourni
     */
    UserDTO creationUtilisateur(LoginDTO loginDTO)
            throws UserAlreadyExistsException;

    /**
     * Permet de se connecter
     * @param loginDTO dto de connexion
     * @param response HttpServletResponse
     * @return Réponse d'authentification contenant le JWT
     */
    AuthResponseDTO login(LoginDTO loginDTO, HttpServletResponse response);

    /**
     * Permet de refresh avec le token de refresh si access token est expiré
     * @param token token
     * @return réponse d'authentification
     */
    AuthResponseDTO refresh(String token);

    /**
     * Se déconnecte
     * @param token token de refresh qui va être supprimé
     * @param response HttpServletResponse
     * @return Déconnexion
     */
    String logout( String token , HttpServletResponse response);

    /**
     * Envoie un mail avec le lien de mise à jour
     * @param email email
     * @return le token
     */
    String forgotPassword(String email);

    /**
     * Mise à jour du mot de passe.
     * @param token le token
     * @param newPassword le nouveau mot de passe
     * @return un message
     */
    String resetPassword( String token , String newPassword);
}
