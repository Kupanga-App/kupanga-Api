package com.kupanga.api.authentification.service.impl;

import com.kupanga.api.email.service.EmailService;
import com.kupanga.api.exception.business.KupangaBusinessException;
import com.kupanga.api.exception.business.UserAlreadyExistsException;
import com.kupanga.api.authentification.dto.AuthResponseDTO;
import com.kupanga.api.authentification.dto.LoginDTO;
import com.kupanga.api.authentification.entity.PasswordResetToken;
import com.kupanga.api.authentification.entity.RefreshToken;
import com.kupanga.api.authentification.service.AuthService;
import com.kupanga.api.authentification.service.PasswordResetTokenService;
import com.kupanga.api.authentification.service.RefreshTokenService;
import com.kupanga.api.authentification.utils.JwtUtils;
import com.kupanga.api.user.dto.formDTO.UserFormDTO;
import com.kupanga.api.authentification.dto.CompleteProfileResponseDTO;
import com.kupanga.api.user.dto.readDTO.UserDTO;
import com.kupanga.api.user.entity.User;
import com.kupanga.api.user.mapper.UserMapper;
import com.kupanga.api.user.service.UserService;
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
import java.time.LocalDateTime;
import java.util.UUID;

import static com.kupanga.api.email.constantes.Constante.RESET_LINK;
import static com.kupanga.api.authentification.constant.AuthConstant.*;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthServiceImpl.class);
    private final UserService userService;
    private final EmailService emailService;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final RefreshTokenService refreshTokenService;
    private final PasswordResetTokenService passwordResetTokenService ;

    @Transactional
    @Override
    public UserDTO creationUtilisateur(LoginDTO loginDTO) throws UserAlreadyExistsException {

        LOGGER.info("Service pour la création du compte utilisateur démarré");

        userService.verifyIfUserExistWithEmail(loginDTO.email());

        User utilisateur = User.builder()
                .mail(loginDTO.email())
                .password(passwordEncoder.encode(loginDTO.password()))
                .build();

        userService.save(utilisateur);
        LOGGER.debug("utilisateur sauvegardé {} " , utilisateur  );

        LOGGER.info(" Fin du service de création du compte utilisateur ");
        return userMapper.toDTO(utilisateur);
    }


    @Override
    public AuthResponseDTO login(LoginDTO loginDTO, HttpServletResponse response) {

        LOGGER.info("Service pour la connexion d'un utilisateur démarré");

        // 1️. Récupérer l'utilisateur
        User utilisateur = userService.getUserByEmail(loginDTO.email());
        LOGGER.debug("Utilisateur {} récupéré avec succès " , utilisateur);

        // 2️. Vérifier le mot de passe
        userService.isCorrectPassword(loginDTO.password(), utilisateur.getPassword());

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

    @Transactional
    @Override
    public String forgotPassword(String email){

        User user = userService.getUserByEmail(email);

        PasswordResetToken passwordResetToken = PasswordResetToken.builder()
                .token(UUID.randomUUID().toString())
                .user(user)
                .expirationDate(LocalDateTime.now().plusMinutes(10))
                .build();
        passwordResetTokenService.save(passwordResetToken);

        emailService.sendPasswordResetMail(email , RESET_LINK + passwordResetToken.getToken());

        return passwordResetToken.getToken();
    }

    @Transactional
    @Override
    public String resetPassword( String token , String newPassword){

        PasswordResetToken passwordResetToken = passwordResetTokenService.getByToken(token);

        if(passwordResetToken.getExpirationDate().isBefore(LocalDateTime.now())){

            throw new RuntimeException("Token expiré");
        }

        User user = passwordResetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userService.save(user);
        passwordResetTokenService.delete(passwordResetToken);

        emailService.sendPasswordUpdatedConfirmation(user.getMail());

        return MOT_DE_PASSE_A_JOUR ;
    }

    @Override
    @Transactional
    public CompleteProfileResponseDTO completeProfil(UserFormDTO userFormDTO , HttpServletResponse response){

        User user = userService.getUserByEmail(userFormDTO.mail());
        userService.verifyIfRoleOfUserValid(userFormDTO.role());
        userService.isCorrectPassword(userFormDTO.password() , user.getPassword());
        user.setRole(userFormDTO.role());
        user.setFirstName(userFormDTO.firstName());
        user.setLastName(userFormDTO.lastName());
        user.setHasCompleteProfil(true);

        userService.save(user);

        LoginDTO loginDTO = LoginDTO.builder()
                .email(userFormDTO.mail())
                .password(userFormDTO.password())
                .build() ;

        AuthResponseDTO authResponseDTO = login(loginDTO ,response );

        emailService.sendWelcomeMessage(user.getMail() , user.getFirstName());

        return CompleteProfileResponseDTO.builder()
                .userDTO(userMapper.toDTO(user))
                .authResponseDTO(authResponseDTO)
                .build();
    }
}
