package com.kupanga.api.login.service.impl;

import com.kupanga.api.email.service.EmailService;
import com.kupanga.api.exception.business.KupangaBusinessException;
import com.kupanga.api.exception.business.UserAlreadyExistsException;
import com.kupanga.api.login.dto.AuthResponseDTO;
import com.kupanga.api.login.dto.LoginDTO;
import com.kupanga.api.login.entity.RefreshToken;
import com.kupanga.api.login.service.LoginService;
import com.kupanga.api.login.service.RefreshTokenService;
import com.kupanga.api.login.utils.JwtUtils;
import com.kupanga.api.utilisateur.dto.readDTO.UserDTO;
import com.kupanga.api.utilisateur.entity.Role;
import com.kupanga.api.utilisateur.entity.User;
import com.kupanga.api.utilisateur.mapper.UserMapper;
import com.kupanga.api.utilisateur.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

import static com.kupanga.api.login.constant.LoginConstant.DECONNEXION;
import static com.kupanga.api.login.constant.LoginConstant.REFRESHTOKEN;

@Service
@RequiredArgsConstructor
public class LoginServiceImpl implements LoginService {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoginServiceImpl.class);
    private final UserService userService;
    private final EmailService emailService;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final RefreshTokenService refreshTokenService;

    @Transactional
    @Override
    public UserDTO creationUtilisateur(String email , Role role) throws UserAlreadyExistsException {

        LOGGER.info("Service pour la création du compte utilisateur démarré");

        userService.verifyIfUserExistWithEmail(email);
        userService.verifyIfRoleOfUserValid(role);

        String motDePasseTemporaire = generationMotDePasseTemporaire();

        User utilisateur = User.builder()
                .mail(email)
                .password(passwordEncoder.encode(motDePasseTemporaire))
                .role(role)
                .build();

        userService.save(utilisateur);
        LOGGER.debug("utilisateur sauvegardé {} " , utilisateur  );

        try {

            emailService.SendPasswordProvisional(email, motDePasseTemporaire);

            LOGGER.info("Email renvoyé à l'utilisateur à l'adresse {} avec le mot de passe temporaire" , email);

        } catch (Exception e) {

            LOGGER.warn("Erreur lors de l'envoi  de l'email de mot de passe temporaire à {} " +
                    "mais l'utilisateur a été crée" , email);

            LOGGER.debug("détail de l'exception : {}" ,e.getMessage());
        }

        LOGGER.info(" Fin du service de création du compte utilisateur ");
        return userMapper.toDTO(utilisateur);
    }

    /**
     * Génère un Mot de passe provisoire
     * @return un mot de passe provisoire
     */

    private String generationMotDePasseTemporaire(){

        return java.util.UUID.randomUUID().toString().substring(0, 8);

    }

    @Override
    public AuthResponseDTO login(LoginDTO loginDTO, HttpServletResponse response) {

        LOGGER.info("Service pour la connexion d'un utilisateur démarré");

        // 1️. Récupérer l'utilisateur
        User utilisateur = userService.getUserByEmail(loginDTO.email());
        LOGGER.debug("Utilisateur {} récupéré avec succès " , utilisateur);

        // 2️. Vérifier le mot de passe
        userService.isCorrectPassword(loginDTO.motDepasse(), utilisateur.getPassword());

        // 3. Générer access token (court)
        String accessToken = jwtUtils.generateAccessToken(
                utilisateur.getMail(),
                String.valueOf(utilisateur.getRole()) // rôle dans le JWT
        );

        // 4️. Générer refresh token (long) et le stocker en DB
        String refreshToken = refreshTokenService.createRefreshToken(utilisateur);

        // 5️. Envoyer le refresh token dans un cookie httpOnly sécurisé
        ResponseCookie refreshCookie = ResponseCookie.from(REFRESHTOKEN, refreshToken)
                .httpOnly(true)
                .secure(true)                // true en production HTTPS
                .sameSite("Strict")
                .path("/auth/refresh")
                .maxAge(Duration.ofDays(14)) // expiration
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());

        LOGGER.info("Service de connexion terminé");

        // 6️. Retourner access token
        return AuthResponseDTO.builder()
                .accessToken(accessToken)
                .build();
    }

    @Override
    public AuthResponseDTO refresh(String token){

        RefreshToken refreshToken = refreshTokenService.getByToken(token);

        if( refreshToken.getRevoked() || refreshToken.getExpiration().isBefore(Instant.now())){

            throw  new KupangaBusinessException("token expiré ou non autorisé " , HttpStatus.UNAUTHORIZED);
        }

        // Génère un nouvel access token

        String newAccessToken = jwtUtils.generateAccessToken(
                refreshToken.getUser().getMail() ,
                refreshToken.getUser().getPassword()
        );

        return AuthResponseDTO.builder()
                .accessToken(newAccessToken)
                .build();
    }

    @Override
    public String logout( String token , HttpServletResponse response){

        if( token != null ){

            // 1. Révoquer le token dans la BD
            refreshTokenService.deleteRefreshToken(token);

            // 2. Supprimer le cookie du navigateur
            ResponseCookie deleteCookie = ResponseCookie.from(REFRESHTOKEN, "")
                    .httpOnly(true)
                    .secure(true)                // true en prod HTTPS
                    .path("/auth/refresh")
                    .maxAge(0)                   // supprime le cookie
                    .build();
            response.addHeader(HttpHeaders.SET_COOKIE, deleteCookie.toString());
        }

        return DECONNEXION ;
    }

}
