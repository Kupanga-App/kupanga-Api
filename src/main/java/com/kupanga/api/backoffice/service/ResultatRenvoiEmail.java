package com.kupanga.api.backoffice.service;

/** M4 : issue d'une demande de renvoi depuis le back-office. */
public enum ResultatRenvoiEmail {
    /** Remis en file : il part dans les secondes qui suivent. */
    REMIS_EN_FILE,
    INTROUVABLE,
    /** Seul un e-mail en échec définitif se renvoie (un e-mail en attente part tout seul, un e-mail envoyé est effacé). */
    PAS_EN_ECHEC,
    /** Le lien qu'il contient a expiré : renvoyer ne servirait à rien (l'utilisateur redemande un lien). */
    LIEN_EXPIRE,
    /** Contenu déjà effacé (RGPD, 30 jours). */
    CONTENU_EFFACE
}
