package com.kupanga.api.juridiction;

/**
 * Devises des montants (code ISO 4217), cf. CLAUDE.md §4bis. Les devises autorisées pour un bien dépendent
 * du profil de juridiction de son pays ({@code kupanga.juridictions.<PAYS>.devises}).
 */
public enum Devise {
    EUR,
    USD,
    /** Franc congolais (RDC). */
    CDF,
    /** Franc CFA d'Afrique centrale (République du Congo). */
    XAF
}
