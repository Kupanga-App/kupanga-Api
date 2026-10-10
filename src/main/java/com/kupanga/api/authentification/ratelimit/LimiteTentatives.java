package com.kupanga.api.authentification.ratelimit;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.BucketConfiguration;
import lombok.Getter;

import java.time.Duration;

/**
 * Seuils de la limite de tentatives sur les routes d'authentification (A3, décisions du 2026-10-08).
 * Par défaut, fenêtre fixe : {@code capacite} essais, rechargés en bloc toutes les {@code periode}.
 * Avec {@code rechargeProgressive}, les essais reviennent un par un, répartis sur la période.
 */
@Getter
public enum LimiteTentatives {

    /** Couple (e-mail, IP) : un tiers qui force un compte ne bloque que lui-même, pas la victime. */
    LOGIN_PAR_EMAIL_ET_IP("login-email-ip", 5, Duration.ofMinutes(15)),
    /** Plafond global par e-mail, contre une attaque répartie sur beaucoup d'IP. */
    LOGIN_PAR_EMAIL("login-email", 30, Duration.ofMinutes(15)),
    LOGIN_PAR_IP("login-ip", 20, Duration.ofMinutes(15)),
    FORGOT_PASSWORD_PAR_EMAIL("forgot-email", 3, Duration.ofHours(1)),
    /** Protège le quota d'e-mails Brevo contre l'envoi de resets vers de nombreux comptes. */
    FORGOT_PASSWORD_PAR_IP("forgot-ip", 20, Duration.ofHours(1)),
    REGISTER_PAR_IP("register-ip", 5, Duration.ofHours(1)),
    /** A14 : une inscription sur une adresse déjà inscrite envoie un e-mail à son titulaire (quota Brevo, spam). */
    REGISTER_PAR_EMAIL("register-email", 3, Duration.ofHours(1)),
    /** A14 : renvoi du lien de confirmation, mêmes seuils que le mot de passe oublié (quota Brevo). */
    RENVOI_VERIFICATION_PAR_EMAIL("verif-email", 3, Duration.ofHours(1)),
    RENVOI_VERIFICATION_PAR_IP("verif-ip", 20, Duration.ofHours(1)),
    GOOGLE_PAR_IP("google-ip", 20, Duration.ofMinutes(15)),
    /**
     * Nouvelles conversations ouvertes par un compte (décision 2026-10-09) : le premier contact révèle l'e-mail
     * du propriétaire (écho W13, liste des conversations) ; ces plafonds freinent la collecte d'adresses et le spam.
     */
    NOUVELLE_CONVERSATION_PAR_HEURE("conversation-heure", 10, Duration.ofHours(1)),
    NOUVELLE_CONVERSATION_PAR_JOUR("conversation-jour", 30, Duration.ofDays(1)),
    /** Login du back-office (BO-LOGIN), par IP. */
    BACKOFFICE_LOGIN_PAR_IP("backoffice-login-ip", 5, Duration.ofMinutes(15)),
    /**
     * Plafond global du compte admin (unique), contre une attaque répartie sur beaucoup d'IP.
     * Recharge progressive (1 essai toutes les 36 s) : un attaquant ne peut pas vider le seau en début de fenêtre
     * et bloquer l'admin pendant une heure ; il lui faut au moins 5 IP actives en continu (5 × 20 / h).
     */
    BACKOFFICE_LOGIN_GLOBAL("backoffice-login", 100, Duration.ofHours(1), true);

    /** Préfixe de la clé de stockage (Redis). */
    private final String prefixe;
    private final int capacite;
    private final Duration periode;
    private final boolean rechargeProgressive;

    LimiteTentatives(String prefixe, int capacite, Duration periode) {
        this(prefixe, capacite, periode, false);
    }

    LimiteTentatives(String prefixe, int capacite, Duration periode, boolean rechargeProgressive) {
        this.prefixe = prefixe;
        this.capacite = capacite;
        this.periode = periode;
        this.rechargeProgressive = rechargeProgressive;
    }

    public BucketConfiguration configuration() {
        var capaciteDefinie = Bandwidth.builder().capacity(capacite);
        Bandwidth limite = rechargeProgressive
                ? capaciteDefinie.refillGreedy(capacite, periode).build()
                : capaciteDefinie.refillIntervally(capacite, periode).build();
        return BucketConfiguration.builder().addLimit(limite).build();
    }
}
