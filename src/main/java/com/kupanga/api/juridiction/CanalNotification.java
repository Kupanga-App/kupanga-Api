package com.kupanga.api.juridiction;

/**
 * Canaux par lesquels les parties sont prévenues (liens de signature, quittances…), selon le pays (§4bis).
 */
public enum CanalNotification {
    /** E-mail envoyé par l'application (Brevo). */
    EMAIL,
    /**
     * Lien {@code wa.me} prérempli, envoyé à la main par le propriétaire depuis son téléphone
     * (décision 2026-10-07 : pas d'envoi automatique de SMS ou WhatsApp).
     */
    WHATSAPP_LIEN
}
