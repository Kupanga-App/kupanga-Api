package com.kupanga.api.email.file;

/** M2 : cycle de vie d'un e-mail en file. */
public enum StatutEmail {
    /** En attente de son prochain essai. */
    A_ENVOYER,
    /** Réservé par un envoyeur jusqu'à {@code verrouJusqua} ; repris après si l'envoyeur a planté. */
    EN_COURS,
    /** Accepté par le serveur SMTP ; contenu effacé. */
    ENVOYE,
    /** Refusé par le serveur (5xx) ou nouveaux essais épuisés ; renvoyable depuis le back-office. */
    ECHEC_DEFINITIF,
    /** Le lien qu'il contenait a expiré avant l'envoi. */
    PERIME
}
