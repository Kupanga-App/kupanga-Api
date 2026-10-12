package com.kupanga.api.immobilier.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Compteurs relevés dans un état des lieux. Ceux proposés dépendent du pays
 * ({@code kupanga.juridictions.<PAYS>.compteurs-edl}) ; C13 ajoute le prépayé et le groupe électrogène.
 */
@Getter
@RequiredArgsConstructor
public enum TypeCompteur {
    EAU_FROIDE("Eau froide"),
    EAU_CHAUDE("Eau chaude"),
    ELECTRICITE_HP("Électricité (heures pleines)"),
    ELECTRICITE_HC("Électricité (heures creuses)"),
    GAZ("Gaz"),
    // ─── C13 ─────────────────────────────────────────────────────────────────
    /** Compteur électrique classique (un seul index). */
    ELECTRICITE("Électricité"),
    /** Compteur prépayé SNEL : relevé = crédit restant (kWh). */
    CASH_POWER("Électricité prépayée (Cash Power)"),
    /** Compteur d'heures du groupe électrogène. */
    GROUPE_ELECTROGENE("Groupe électrogène (heures)");

    /** Affiché dans le PDF de l'état des lieux. */
    private final String libelle;
}
