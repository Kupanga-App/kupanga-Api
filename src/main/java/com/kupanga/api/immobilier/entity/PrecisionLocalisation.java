package com.kupanga.api.immobilier.entity;

/**
 * C2 : d'où vient la position d'un bien, de la plus précise à l'absence de position.
 * Seules {@link #REPERE} et {@link #ADRESSE} donnent lieu au calcul des POI (un centre de commune ou de ville
 * donnerait des commerces et écoles qui ne sont pas autour du logement).
 */
public enum PrecisionLocalisation {
    /** Repère placé sur la carte ou GPS du téléphone du propriétaire. */
    REPERE,
    /** Adresse trouvée par le géocodage. */
    ADRESSE,
    /** Centre du quartier (adresse non trouvée). */
    QUARTIER,
    /** Centre de la commune. */
    COMMUNE,
    /** Centre de la ville. */
    VILLE,
    /** Aucune position : bien absent de la carte. */
    AUCUNE;

    public boolean estPrecise() {
        return this == REPERE || this == ADRESSE;
    }
}
