package com.kupanga.api.backoffice.service;

/**
 * B12 : issue de la suppression d'un compte depuis le back-office.
 */
public enum ResultatSuppressionCompte {
    /** Aucune donnée liée : compte, conversations et messages supprimés. */
    SUPPRIME,
    /** Données liées (biens, baux, quittances, EDL) : identité effacée, documents conservés. */
    ANONYMISE,
    DEJA_ANONYMISE,
    INTROUVABLE
}
