package com.kupanga.api.immobilier.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** État constaté d'un élément de l'état des lieux ; {@code libelle} est le texte affiché sur le PDF. */
@Getter
@RequiredArgsConstructor
public enum EtatElement {
    BON("Bon état"),
    USAGE_NORMAL("Usure normale"),
    MAUVAIS("Mauvais état"),
    HORS_SERVICE("Hors service");

    private final String libelle;
}
