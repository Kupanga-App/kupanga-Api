package com.kupanga.api.authentification.ratelimit;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.BucketConfiguration;
import lombok.Getter;

import java.time.Duration;

/**
 * Seuils de la limite de tentatives sur les routes d'authentification (A3, décisions du 2026-10-08).
 * Chaque limite est une fenêtre fixe : {@code capacite} essais, rechargés en bloc toutes les {@code periode}.
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
    GOOGLE_PAR_IP("google-ip", 20, Duration.ofMinutes(15));

    /** Préfixe de la clé de stockage (Redis). */
    private final String prefixe;
    private final int capacite;
    private final Duration periode;

    LimiteTentatives(String prefixe, int capacite, Duration periode) {
        this.prefixe = prefixe;
        this.capacite = capacite;
        this.periode = periode;
    }

    public BucketConfiguration configuration() {
        return BucketConfiguration.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(capacite)
                        .refillIntervally(capacite, periode)
                        .build())
                .build();
    }
}
