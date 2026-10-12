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
}
