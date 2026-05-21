package com.kupanga.api.authentification.service;

import com.kupanga.api.authentification.dto.AuthResponseDTO;
import com.kupanga.api.authentification.dto.CompleteGoogleProfileDTO;
import com.kupanga.api.authentification.dto.GoogleLoginDTO;
import com.kupanga.api.authentification.dto.LoginDTO;
import com.kupanga.api.user.dto.formDTO.UserFormDTO;
import com.kupanga.api.user.dto.readDTO.UserDTO;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;


public interface AuthService {

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


    /**
     * Complète le profil de l'utilisateur
     * @param userFormDTO le formulaire
     * @param imageProfil image
     * @param response HttpServletResponse
     * @return le DTO contenant UserDTO et le token d'authentification
     */
    AuthResponseDTO createAndCompleteUserProfil(UserFormDTO userFormDTO , MultipartFile imageProfil, HttpServletResponse response);

    /**
     * Retourne les infos de l'utilisateur
     * @param email email utilisateur
     * @return les infos de l'utilisateur
     */
    UserDTO getUserInfos(String email);

    /**
     * Connexion via Google OAuth2.
     * Si l'utilisateur n'existe pas, il est créé sans rôle et requiresRoleSelection=true est retourné.
     * Si l'utilisateur existe, connexion normale avec requiresRoleSelection=false.
     * @param dto dto contenant l'ID token Google
     * @param response HttpServletResponse pour le cookie refresh
     * @return AuthResponseDTO avec accessToken et requiresRoleSelection
     */
    AuthResponseDTO loginWithGoogle(GoogleLoginDTO dto, HttpServletResponse response);

    /**
     * Complète le profil d'un utilisateur Google en assignant son rôle.
     * @param dto dto contenant le rôle choisi
     * @param email email de l'utilisateur (extrait du JWT)
     * @param response HttpServletResponse pour le cookie refresh
     * @return AuthResponseDTO avec accessToken et requiresRoleSelection=false
     */
    AuthResponseDTO completeGoogleProfile(CompleteGoogleProfileDTO dto, String email, HttpServletResponse response);
}
