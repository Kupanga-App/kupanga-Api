package com.kupanga.api.authentification.service.impl;

import com.kupanga.api.authentification.dto.AuthResponseDTO;
import com.kupanga.api.authentification.dto.CompleteGoogleProfileDTO;
import com.kupanga.api.authentification.dto.GoogleLoginDTO;
import com.kupanga.api.authentification.dto.LoginDTO;
import com.kupanga.api.authentification.entity.PasswordResetToken;
import com.kupanga.api.authentification.entity.RefreshToken;
import com.kupanga.api.authentification.google.GoogleTokenVerifier;
import com.kupanga.api.authentification.google.GoogleUserInfo;
import com.kupanga.api.authentification.service.AuthService;
import com.kupanga.api.authentification.service.VerificationEmailService;
import com.kupanga.api.authentification.service.PasswordResetTokenService;
import com.kupanga.api.authentification.service.RefreshTokenService;
import com.kupanga.api.authentification.utils.JwtUtils;
import com.kupanga.api.email.service.EmailService;
import com.kupanga.api.exception.business.InvalidPasswordException;
import com.kupanga.api.exception.business.KupangaBusinessException;
import com.kupanga.api.minio.image.ValidationImage;
import com.kupanga.api.minio.service.MinioService;
import com.kupanga.api.user.dto.formDTO.UserFormDTO;
import com.kupanga.api.user.dto.readDTO.UserDTO;
import com.kupanga.api.user.entity.User;
import com.kupanga.api.user.mapper.UserMapper;
import com.kupanga.api.user.service.UserService;
import com.kupanga.api.user.utils.EmailUtils;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.multipart.MultipartFile;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static com.kupanga.api.authentification.constant.AuthConstant.*;
import static com.kupanga.api.minio.constant.MinioConstant.PHOTO_PROFIL_BUCKET;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthServiceImpl.class);
    private final UserService          userService;
    private final EmailService         emailService;
    private final UserMapper           userMapper;
    private final PasswordEncoder      passwordEncoder;
    private final JwtUtils             jwtUtils;
    private final RefreshTokenService  refreshTokenService;
    private final PasswordResetTokenService passwordResetTokenService;
    private final MinioService         minioService;
    private final GoogleTokenVerifier  googleTokenVerifier;
    private final VerificationEmailService verificationEmailService;
    @Value("${app.cookie.secure}")
    private boolean cookieSecure;

    @Value("${app.cookie.same-site}")
    private String cookieSameSite;

    /** Hash BCrypt factice : comparé quand l'e-mail est inconnu, pour que le login prenne le même temps (A2). */
    private String hashFactice;

    @PostConstruct
    void initHashFactice() {
        hashFactice = passwordEncoder.encode(UUID.randomUUID().toString());
    }


    @Override
    public AuthResponseDTO login(LoginDTO loginDTO, HttpServletResponse response) {

        LOGGER.info("Service pour la connexion d'un utilisateur démarré");

        // 1. Récupérer l'utilisateur (même erreur 401 que pour un mauvais mot de passe : pas d'énumération des comptes)
        User utilisateur = userService.findOptionalByMail(loginDTO.email())
                .orElseThrow(() -> {
                    // Calcul BCrypt quand même : même temps de réponse que pour un mauvais mot de passe
                    passwordEncoder.matches(loginDTO.password(), hashFactice);
                    return new InvalidPasswordException();
                });
        LOGGER.debug("Utilisateur id={} récupéré avec succès", utilisateur.getId());

        // 2. Vérifier le mot de passe
        userService.isCorrectPassword(loginDTO.password(), utilisateur.getPassword());

        // A14 : adresse non confirmée → 403 (seulement après le bon mot de passe : pas d'énumération des comptes)
        if (!utilisateur.isEmailVerifie()) {
            throw new KupangaBusinessException(EMAIL_NON_VERIFIE, HttpStatus.FORBIDDEN);
        }

        // 3. Générer access token (court)
        String accessToken = jwtUtils.generateAccessToken(
                utilisateur.getMail(),
                String.valueOf(utilisateur.getRole())
        );

        // 4. Générer refresh token (long) et le stocker en DB
        String refreshToken = refreshTokenService.createRefreshToken(utilisateur);

        // 5. Envoyer le refresh token dans un cookie httpOnly
        addRefreshCookie(response, refreshToken);

        LOGGER.info("Service de connexion terminé");

        // 6. Retourner access token
        return AuthResponseDTO.builder()
                .accessToken(accessToken)
                .requiresRoleSelection(false)
                .build();
    }

    @Override
    public AuthResponseDTO refresh(@CookieValue(name = REFRESHTOKEN, required = false) String token){

        RefreshToken refreshToken = refreshTokenService.getByToken(token);

        if( refreshToken.getRevoked() || refreshToken.getExpiration().isBefore(Instant.now())){

            throw  new KupangaBusinessException("token expiré ou non autorisé " , HttpStatus.UNAUTHORIZED);
        }

        // Génère un nouvel access token (rôle vide tant qu'un compte Google n'a pas choisi son rôle)

        User user = refreshToken.getUser();
        boolean requiresRoleSelection = (user.getRole() == null);
        String roleStr = user.getRole() != null ? String.valueOf(user.getRole()) : "";

        String newAccessToken = jwtUtils.generateAccessToken(user.getMail(), roleStr);

        return AuthResponseDTO.builder()
                .accessToken(newAccessToken)
                .requiresRoleSelection(requiresRoleSelection)
                .build();
    }

    @Override
    public String logout(String token, HttpServletResponse response) {

        if (token != null) {

            // 1. Révoquer le token dans la BD
            refreshTokenService.deleteRefreshToken(token);

            // 2. Supprimer le cookie du navigateur
            ResponseCookie deleteCookie = ResponseCookie.from(REFRESHTOKEN, "")
                    .httpOnly(true)
                    .secure(cookieSecure)       // false en local, true en prod
                    .sameSite(cookieSameSite)   // "Lax" en local, "None" en prod
                    .path("/")
                    .maxAge(0)                  // supprime le cookie
                    .build();
            response.addHeader(HttpHeaders.SET_COOKIE, deleteCookie.toString());
        }

        return DECONNEXION;
    }

    @Transactional
    @Override
    public String forgotPassword(String email){

        // Même réponse que le compte existe ou non : pas d'énumération des comptes
        // B12 : un compte anonymisé (adresse .invalid) n'est jamais réactivé ni écrit
        User user = userService.findOptionalByMail(email).filter(u -> !u.isAnonymise()).orElse(null);
        if (user == null) {
            LOGGER.info("Demande de réinitialisation pour un e-mail inconnu, ignorée");
            return MAIL_REINITIALISATION_ENVOYE;
        }

        passwordResetTokenService.deleteIfExist(user.getId());

        PasswordResetToken passwordResetToken = PasswordResetToken.builder()
                .token(UUID.randomUUID().toString())
                .user(user)
                .expirationDate(LocalDateTime.now().plusMinutes(10))
                .build();
        passwordResetTokenService.save(passwordResetToken);

        emailService.sendPasswordResetMail(user.getMail() , passwordResetToken.getToken());

        return MAIL_REINITIALISATION_ENVOYE;
    }

    @Transactional
    @Override
    public String resetPassword( String token , String newPassword){

        PasswordResetToken passwordResetToken = passwordResetTokenService.getByToken(token);

        if(passwordResetToken.getExpirationDate().isBefore(LocalDateTime.now())){

            throw new KupangaBusinessException(TOKEN_REINITIALISATION_INVALIDE, HttpStatus.BAD_REQUEST);
        }

        User user = passwordResetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        // Le lien de réinitialisation a été reçu à cette adresse : elle appartient bien à l'utilisateur (A14)
        user.setEmailVerifie(true);
        userService.save(user);
        passwordResetTokenService.delete(passwordResetToken);
        verificationEmailService.annulerLien(user.getId());

        // Déconnecte toutes les sessions existantes (un attaquant éventuel perd son refresh token)
        refreshTokenService.revokeAllForUser(user);

        emailService.sendPasswordUpdatedConfirmation(user.getMail());

        return MOT_DE_PASSE_A_JOUR ;
    }

    @Override
    @Transactional
    public String createAndCompleteUserProfil(UserFormDTO userFormDTO , MultipartFile imageProfil){

        // Contrôles du formulaire avant de chercher le compte : mêmes erreurs que l'adresse soit inscrite ou non
        userService.verifyIfRoleOfUserValid(userFormDTO.role());
        String url = userFormDTO.urlAvatar();
        // Revue B5 : pas d'URL externe (pixel de suivi, contenu non contrôlé) en photo de profil publique
        if (url != null && !url.isBlank() && !minioService.estUrlDuBucket(url, PHOTO_PROFIL_BUCKET)) {
            throw new KupangaBusinessException("Avatar invalide", HttpStatus.BAD_REQUEST);
        }
        // Photo contrôlée (B5) même si l'adresse est déjà inscrite : sinon 415 seulement pour une adresse libre
        if (imageProfil != null && !imageProfil.isEmpty()) {
            ValidationImage.verifier(imageProfil);
        }
        // Calcul BCrypt dans tous les cas : temps de réponse proche que le compte existe ou non
        String motDePasseHache = passwordEncoder.encode(userFormDTO.password());

        // A14 : adresse déjà inscrite → même réponse qu'une inscription (pas d'énumération des comptes) ;
        // son titulaire est invité à passer par « mot de passe oublié », vérifié ou non : un nouveau lien de
        // confirmation validerait le mot de passe choisi par le premier inscrit, peut-être un tiers
        Optional<User> existant = userService.findOptionalByMail(userFormDTO.mail());
        if (existant.isPresent()) {
            LOGGER.info("Inscription demandée avec l'adresse du compte {}, déjà inscrite", existant.get().getId());
            emailService.envoyerTentativeInscription(existant.get().getMail());
            return COMPTE_CREE_VERIFIER_EMAIL;
        }

        User user = new User();
        user.setMail(EmailUtils.normaliser(userFormDTO.mail()));
        user.setPassword(motDePasseHache);
        user.setRole(userFormDTO.role());
        user.setFirstName(userFormDTO.firstName());
        user.setLastName(userFormDTO.lastName());
        if( imageProfil != null && !imageProfil.isEmpty()){
            url = minioService.uploadImage(imageProfil , PHOTO_PROFIL_BUCKET);
        }

        user.setUrlProfile(url);
        user.setHasCompleteProfil(true);
        user.setEmailVerifie(false);

        userService.save(user);

        // A14 : plus de connexion automatique ; le lien de confirmation part après le commit,
        // l'e-mail de bienvenue une fois l'adresse confirmée
        verificationEmailService.envoyerLien(user);

        return COMPTE_CREE_VERIFIER_EMAIL;
    }

    @Override
    public UserDTO getUserInfos(String email){

        return userMapper.toDTO(userService.getUserByEmail(email)) ;
    }

    @Override
    @Transactional
    public AuthResponseDTO loginWithGoogle(GoogleLoginDTO dto, HttpServletResponse response) {

        GoogleUserInfo googleInfo = googleTokenVerifier.verify(dto.idToken());

        // Cherche par googleId → puis par email (compte existant à lier) → sinon crée
        User user = userService.findOptionalByGoogleId(googleInfo.googleId())
                .orElseGet(() -> userService.findOptionalByMail(googleInfo.email())
                        .map(existing -> {
                            if (!existing.isEmailVerifie()) {
                                reprendreCompteNonVerifie(existing, googleInfo);
                            }
                            existing.setGoogleId(googleInfo.googleId());
                            userService.save(existing);
                            return existing;
                        })
                        .orElseGet(() -> {
                            User newUser = User.builder()
                                    .googleId(googleInfo.googleId())
                                    .mail(EmailUtils.normaliser(googleInfo.email()))
                                    .firstName(googleInfo.firstName())
                                    .lastName(googleInfo.lastName())
                                    .urlProfile(googleInfo.pictureUrl())
                                    .hasCompleteProfil(false)
                                    .emailVerifie(true) // adresse confirmée par Google (email_verified contrôlé par GoogleTokenVerifierImpl, A4)
                                    .build();
                            userService.save(newUser);
                            return newUser;
                        })
                );

        boolean requiresRoleSelection = (user.getRole() == null);
        String roleStr = user.getRole() != null ? String.valueOf(user.getRole()) : "";

        String accessToken  = jwtUtils.generateAccessToken(user.getMail(), roleStr);
        String refreshToken = refreshTokenService.createRefreshToken(user);
        addRefreshCookie(response, refreshToken);

        LOGGER.info("[GOOGLE-AUTH] Connexion réussie pour le compte {} — sélection rôle requise : {}", user.getId(), requiresRoleSelection);

        return AuthResponseDTO.builder()
                .accessToken(accessToken)
                .requiresRoleSelection(requiresRoleSelection)
                .build();
    }

    @Override
    @Transactional
    public AuthResponseDTO completeGoogleProfile(CompleteGoogleProfileDTO dto, String email, HttpServletResponse response) {

        User user = userService.getUserByEmail(email);

        if (user.getGoogleId() == null) {
            throw new KupangaBusinessException(
                    "Cet endpoint est réservé aux comptes Google", HttpStatus.BAD_REQUEST);
        }
        if (user.getRole() != null) {
            throw new KupangaBusinessException(
                    "Le profil est déjà complété", HttpStatus.BAD_REQUEST);
        }

        userService.verifyIfRoleOfUserValid(dto.role());

        user.setRole(dto.role());
        user.setHasCompleteProfil(true);
        userService.save(user);

        String accessToken  = jwtUtils.generateAccessToken(user.getMail(), String.valueOf(user.getRole()));
        String refreshToken = refreshTokenService.createRefreshToken(user);
        addRefreshCookie(response, refreshToken);

        LOGGER.info("[GOOGLE-AUTH] Profil complété pour le compte {} — rôle : {}", user.getId(), dto.role());

        emailService.sendWelcomeMessage(user.getMail() , user.getFirstName());

        return AuthResponseDTO.builder()
                .accessToken(accessToken)
                .requiresRoleSelection(false)
                .build();
    }

    /**
     * A14 : un compte local jamais confirmé a pu être créé par un tiers avec l'adresse de la personne qui se
     * connecte avec Google (décision du 2026-10-09). Google atteste l'adresse : le compte lui revient, sans rien
     * de ce que le premier inscrit a saisi (mot de passe, rôle, profil), ni lien ou session en cours.
     */
    private void reprendreCompteNonVerifie(User user, GoogleUserInfo googleInfo) {
        user.setPassword(null);
        user.setRole(null);
        user.setHasCompleteProfil(false);
        user.setFirstName(googleInfo.firstName());
        user.setLastName(googleInfo.lastName());
        user.setUrlProfile(googleInfo.pictureUrl());
        user.setEmailVerifie(true);
        verificationEmailService.annulerLien(user.getId());
        passwordResetTokenService.deleteIfExist(user.getId());
        refreshTokenService.revokeAllForUser(user);
        LOGGER.info("[GOOGLE-AUTH] Compte {} non confirmé repris par la connexion Google", user.getId());
    }

    private void addRefreshCookie(HttpServletResponse response, String refreshToken) {
        ResponseCookie cookie = ResponseCookie.from(REFRESHTOKEN, refreshToken)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(cookieSameSite)
                .path("/")
                .maxAge(Duration.ofDays(14))
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
