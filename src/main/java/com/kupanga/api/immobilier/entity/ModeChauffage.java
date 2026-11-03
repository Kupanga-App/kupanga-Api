package com.kupanga.api.immobilier.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** Mode de chauffage du logement ; {@code libelle} est le texte affiché sur les documents PDF. */
@Getter
@RequiredArgsConstructor
public enum ModeChauffage {
    ELECTRIQUE("électrique"),
    GAZ("gaz"),
    FIOUL("fioul"),
    BOIS("bois"),
    POMPE_A_CHALEUR("pompe à chaleur"),
    POELE("poêle"),
    COLLECTIF("collectif"),
    SANS_CHAUFFAGE("sans chauffage");

    private final String libelle;
}
