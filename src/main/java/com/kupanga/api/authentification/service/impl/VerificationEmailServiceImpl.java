package com.kupanga.api.authentification.service.impl;

import com.kupanga.api.authentification.entity.JetonVerificationEmail;
import com.kupanga.api.authentification.repository.JetonVerificationEmailRepository;
import com.kupanga.api.authentification.service.VerificationEmailService;
import com.kupanga.api.email.service.EmailService;
import com.kupanga.api.exception.business.KupangaBusinessException;
import com.kupanga.api.user.entity.User;
import com.kupanga.api.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

import static com.kupanga.api.authentification.constant.AuthConstant.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class VerificationEmailServiceImpl implements VerificationEmailService {

    /** Durée de validité du lien (décision du 2026-10-09). */
    static final Duration VALIDITE = Duration.ofHours(24);

    private final JetonVerificationEmailRepository jetonRepository;
    private final UserService userService;
    private final EmailService emailService;

    @Override
    @Transactional
    public void envoyerLien(User user) {
        jetonRepository.deleteByUserId(user.getId());
        JetonVerificationEmail jeton = jetonRepository.save(JetonVerificationEmail.builder()
                .token(UUID.randomUUID().toString())
                .user(user)
                .expiration(LocalDateTime.now().plus(VALIDITE))
                .build());
        // Envoyé après le commit (B11) : pas d'e-mail pour un compte dont la création est annulée
        emailService.envoyerVerificationEmail(user.getMail(), user.getFirstName(), jeton.getToken());
    }

    @Override
    @Transactional
    public String verifier(String token) {
        JetonVerificationEmail jeton = jetonRepository.findByToken(token)
                .orElseThrow(() -> new KupangaBusinessException(LIEN_VERIFICATION_INVALIDE, HttpStatus.BAD_REQUEST));
        if (jeton.getExpiration().isBefore(LocalDateTime.now())) {
            throw new KupangaBusinessException(LIEN_VERIFICATION_INVALIDE, HttpStatus.BAD_REQUEST);
        }

        User user = jeton.getUser();
        user.setEmailVerifie(true);
        userService.save(user);
        jetonRepository.delete(jeton);
        log.info("Adresse e-mail vérifiée pour le compte {}", user.getId());

        emailService.sendWelcomeMessage(user.getMail(), user.getFirstName());
        return EMAIL_VERIFIE;
    }

    @Override
    @Transactional
    public void annulerLien(Long userId) {
        jetonRepository.deleteByUserId(userId);
    }

    @Override
    @Transactional
    public String renvoyer(String email) {
        // Même réponse que le compte existe, soit déjà vérifié ou non : pas d'énumération des comptes
        userService.findOptionalByMail(email)
                .filter(user -> !user.isEmailVerifie() && !user.isAnonymise())
                .ifPresent(this::envoyerLien);
        return LIEN_VERIFICATION_ENVOYE;
    }
}
