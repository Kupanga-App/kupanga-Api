package com.kupanga.api.immobilier.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * C11 : équipements d'un bien. Ceux proposés dépendent du pays ({@code kupanga.juridictions.<PAYS>.equipements}) :
 * à Kinshasa, l'accès à l'eau et à l'électricité compte autant que la surface.
 */
@Getter
@RequiredArgsConstructor
public enum Equipement {
    // ─── Eau ─────────────────────────────────────────────────────────────────
    EAU_REGIDESO("Eau courante (REGIDESO)"),
    FORAGE("Forage"),
    CITERNE("Citerne"),
    // ─── Électricité ─────────────────────────────────────────────────────────
    ELECTRICITE_SNEL("Électricité (SNEL)"),
    GROUPE_ELECTROGENE("Groupe électrogène"),
    PANNEAUX_SOLAIRES("Panneaux solaires"),
    // ─── Sécurité et confort ─────────────────────────────────────────────────
    CLOTURE("Clôture / mur"),
    GARDIENNAGE("Gardiennage"),
    PARKING("Parking"),
    CLIMATISATION("Climatisation");

    private final String libelle;
}
