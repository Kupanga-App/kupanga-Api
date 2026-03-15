package com.kupanga.api.immobilier.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PoiType {

    SCHOOL(     "school",     "amenity", "École"),
    HOSPITAL(   "hospital",   "amenity", "Hôpital"),
    PHARMACY(   "pharmacy",   "amenity", "Pharmacie"),
    KINDERGARTEN("kindergarten", "amenity", "Garderie d'enfants");

    private final String osmValue;    // valeur OSM : "school", "hospital"...
    private final String osmKey;      // clé OSM   : "amenity"
    private final String labelFr;     // libellé lisible pour le front
}
