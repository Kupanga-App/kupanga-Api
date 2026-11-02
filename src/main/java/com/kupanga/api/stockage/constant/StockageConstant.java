package com.kupanga.api.stockage.constant;

import java.time.Duration;
import java.util.List;

public final class StockageConstant {

    public static final String PHOTO_PROFIL_BUCKET = "bucket-photo-profil";
    public static final String PHOTO_IMO_BUCKET = "bucket-photo-immobilier";
    public static final String CONTRAT_BUCKET = "contrat-de-bail";
    public static final String EDL_BUCKET = "bucket-etats-des-lieux";
    public static final String QUITTANCE_BUCKET = "bucket-des-quittances";
    /** C8-bis : photos des reçus de paiement (données personnelles). */
    public static final String RECU_BUCKET = "bucket-recus-paiement";

    /** Buckets contenant des données personnelles : toujours privés (P0-7). */
    /** S2 : photos de profil comprises (données personnelles, lues par URL présignée de 24 h). */
    public static final List<String> BUCKETS_PRIVES =
            List.of(CONTRAT_BUCKET, EDL_BUCKET, QUITTANCE_BUCKET, RECU_BUCKET, PHOTO_PROFIL_BUCKET);

    /** Buckets lisibles sans authentification (photos des biens, affichées dans les annonces). */
    public static final List<String> BUCKETS_PUBLICS = List.of(PHOTO_IMO_BUCKET);

    /** Durée de validité d'une URL présignée. */
    public static final int DUREE_URL_PRESIGNEE_MINUTES = 5;

    /**
     * S2 : URL présignée d'une photo de profil (décision 2026-11-02) : 24 h, car le front la garde dans le
     * localStorage et le cache hors ligne (R2) ; les documents restent à 5 minutes.
     */
    public static final Duration DUREE_URL_PHOTO_PROFIL = Duration.ofHours(24);

    private StockageConstant() {
    }
}
