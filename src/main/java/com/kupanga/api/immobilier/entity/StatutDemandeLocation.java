package com.kupanga.api.immobilier.entity;

/**
 * État d'une demande de location envoyée par un propriétaire à un candidat.
 * Une demande {@code EN_ATTENTE} dont la date d'expiration est passée est traitée comme {@code EXPIREE}
 * (le statut enregistré est mis à jour à la prochaine demande sur le même bien).
 */
public enum StatutDemandeLocation {
    EN_ATTENTE,
    ACCEPTEE,
    REFUSEE,
    ANNULEE,
    EXPIREE
}
