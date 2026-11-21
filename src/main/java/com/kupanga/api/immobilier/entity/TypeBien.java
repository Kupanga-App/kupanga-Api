package com.kupanga.api.immobilier.entity;

import java.util.Set;

/**
 * Types de bien. Ceux proposés dépendent du pays ({@code kupanga.juridictions.<PAYS>.types-bien}) :
 * C10 ajoute les types en usage à Kinshasa (villa, annexe, chambre, local commercial, entrepôt, parcelle, terrain).
 */
public enum TypeBien {

    APPARTEMENT("Appartement"),
    MAISON("Maison"),
    STUDIO("Studio"),
    // ─── C10 (RDC) ───────────────────────────────────────────────────────────
    VILLA("Villa"),
    /** Logement secondaire dans la parcelle du bailleur. */
    ANNEXE("Annexe"),
    CHAMBRE("Chambre"),
    LOCAL_COMMERCIAL("Local commercial"),
    ENTREPOT("Entrepôt"),
    /** Terrain bâti ou clôturé, avec ses constructions. */
    PARCELLE("Parcelle"),
    /** Terrain nu. */
    TERRAIN("Terrain");

    /** Libellé affiché dans les documents (modèles RDC). */
    private final String libelle;

    TypeBien(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }

    private static final Set<TypeBien> SANS_LOGEMENT = Set.of(PARCELLE, TERRAIN);

    /** C10 : surface habitable et nombre de pièces ne sont exigés que pour un bien avec logement. */
    public boolean estSansLogement() {
        return SANS_LOGEMENT.contains(this);
    }

    /**
     * Usage du bien, qui détermine le régime juridique du bail (RDC) : habitation (loi n° 15/025 du 31 décembre 2015
     * relative aux baux à loyer non professionnels), usage professionnel (Acte uniforme OHADA portant sur le droit
     * commercial général, art. 101 à 134) ou terrain nu (droit commun du louage).
     * Une parcelle (terrain avec ses constructions) est louée comme habitation.
     */
    public UsageBail usageBail() {
        return switch (this) {
            case LOCAL_COMMERCIAL, ENTREPOT -> UsageBail.PROFESSIONNEL;
            case TERRAIN -> UsageBail.TERRAIN_NU;
            default -> UsageBail.HABITATION;
        };
    }

    public enum UsageBail { HABITATION, PROFESSIONNEL, TERRAIN_NU }
}
