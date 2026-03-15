package com.kupanga.api.immobilier.service;

import com.kupanga.api.immobilier.entity.Bien;

public interface BienPoiService {

    /**
     * Calcule et sauvegarde les POI pour un bien de façon asynchrone.
     * @Async → ne bloque pas la création du bien
     */
    void calculerEtSauvegarderPoi(Bien bien);
}
