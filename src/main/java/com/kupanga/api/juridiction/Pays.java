package com.kupanga.api.juridiction;

import lombok.Getter;

import java.util.Locale;

/**
 * J1 : pays d'un bien, code ISO 3166-1 alpha-2 (cf. CLAUDE.md §4bis).
 * Le pays sélectionne le profil de juridiction (devises, champs, modèles de documents, notifications) :
 * le code lit ce profil, jamais de {@code if (pays == ...)} dans les services.
 */
@Getter
public enum Pays {

    FR("France"),
    BE("Belgique"),
    CD("République démocratique du Congo"),
    CG("République du Congo");

    /** Nom affiché en français (back-office, documents). */
    private final String libelle;

    Pays(String libelle) {
        this.libelle = libelle;
    }

    /** Code en minuscules attendu par les services de géocodage ({@code countrycodes=fr}). */
    public String codeMinuscule() {
        return name().toLowerCase(Locale.ROOT);
    }
}
