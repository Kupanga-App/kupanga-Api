package com.kupanga.api.minio.constant;

import java.util.List;

public class MinioConstant {

    public static final String PHOTO_PROFIL_BUCKET = "bucket-photo-profil";
    public static final String PHOTO_IMO_BUCKET = "bucket-photo-immobilier";
    public static final String CONTRAT_BUCKET = "contrat-de-bail";
    public static final String EDL_BUCKET = "bucket-etats-des-lieux";
    public static final String QUITTANCE_BUCKET = "bucket-des-quittances";

    /** Buckets contenant des données personnelles : toujours privés (P0-7). */
    public static final List<String> BUCKETS_PRIVES = List.of(CONTRAT_BUCKET, EDL_BUCKET, QUITTANCE_BUCKET);

    /** Durée de validité d'une URL présignée. */
    public static final int DUREE_URL_PRESIGNEE_MINUTES = 5;
}
