package com.kupanga.api.authentification.service;

import com.kupanga.api.authentification.dto.AuthResponseDTO;
import com.kupanga.api.authentification.dto.LoginDTO;
import com.kupanga.api.user.dto.formDTO.UserFormDTO;
import com.kupanga.api.authentification.dto.CompleteProfileResponseDTO;
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
    CompleteProfileResponseDTO createAndCompleteUserProfil(UserFormDTO userFormDTO , MultipartFile imageProfil, HttpServletResponse response);

    UserDTO getUserInfos(String email);
}
