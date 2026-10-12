package com.kupanga.api.immobilier.service;

import com.kupanga.api.immobilier.entity.Bien;
import com.kupanga.api.minio.image.PhotoPreparee;

import java.util.List;

public interface BienImageService {

    /**
     * Envoie les photos du bien, déjà contrôlées et préparées (R1 : 1600 px + vignette de 400 px).
     * @param photos photos préparées
     * @param bucketName nom du bucket
     * @param bien le bien.
     */
    void uploadImagesImo(List<PhotoPreparee> photos, String bucketName,  Bien bien);
}
