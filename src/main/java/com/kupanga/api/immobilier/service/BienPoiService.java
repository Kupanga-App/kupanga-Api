package com.kupanga.api.immobilier.service;

import com.kupanga.api.immobilier.entity.Bien;

public interface BienPoiService {

    /**
     * Calcule et sauvegarde les POI pour un bien de façon asynchrone.
     * S'exécute en arrière-plan et ne bloque pas la création du bien.
     *
     * @param bien le bien pour lequel calculer les POI
     */
    void calculerEtSauvegarderPoi(Bien bien);
}
