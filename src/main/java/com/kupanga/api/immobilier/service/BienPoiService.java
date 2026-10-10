package com.kupanga.api.immobilier.service;

public interface BienPoiService {

    /**
     * Calcule et sauvegarde les POI pour un bien de façon asynchrone.
     * S'exécute en arrière-plan et ne bloque pas la création du bien.
     *
     * @param bienId l'id du bien (B9 : le bien est relu dans le thread asynchrone, jamais une entité détachée)
     */
    void calculerEtSauvegarderPoi(Long bienId);
}
