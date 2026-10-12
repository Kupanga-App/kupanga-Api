package com.kupanga.api.immobilier.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * C8-bis : comment le locataire a payé, déclaré par le propriétaire. Aucun paiement ne passe par l'application :
 * c'est une simple information. Les modes proposés dépendent du pays ({@code kupanga.juridictions.<PAYS>.modes-paiement}).
 */
@Getter
@RequiredArgsConstructor
public enum ModePaiement {
    ESPECES("en espèces"),
    MOBILE_MONEY("par Mobile Money"),
    VIREMENT("par virement"),
    CHEQUE("par chèque"),
    AUTRE("par un autre moyen");

    /** Complément affiché après « Payée le … » (PDF de la quittance). */
    private final String libelle;
}
