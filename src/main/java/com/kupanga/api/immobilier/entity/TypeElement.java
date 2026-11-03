package com.kupanga.api.immobilier.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** Élément inspecté dans une pièce de l'état des lieux ; {@code libelle} est le texte affiché sur le PDF. */
@Getter
@RequiredArgsConstructor
public enum TypeElement {
    MUR("Murs"),
    PLAFOND("Plafond"),
    SOL("Sol"),
    FENETRE("Fenêtres"),
    PORTE("Portes"),
    VOLET("Volets"),
    PRISE("Prises électriques"),
    LUMINAIRE("Luminaires"),
    RADIATEUR("Radiateurs"),
    EQUIPEMENT("Équipement"),
    AUTRE("Autre");

    private final String libelle;
}
