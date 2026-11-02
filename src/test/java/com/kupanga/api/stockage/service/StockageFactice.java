package com.kupanga.api.stockage.service;

import com.kupanga.api.stockage.image.PhotoPreparee;
import com.kupanga.api.stockage.image.PhotoStockee;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;

/**
 * S2 : stockage simulé pour les tests des mappers : URL publique {@code https://fichiers.test/<bucket>/<clé>},
 * URL présignée {@code ...?X-Amz-Expires=<secondes>&X-Amz-Signature=test}. Aucun envoi.
 */
public class StockageFactice implements StockageService {

    public static final String URL = "https://fichiers.test/";

    @Override
    public String urlPublique(String bucketName, String cle) {
        return cle == null || cle.isBlank() ? null : URL + bucketName + "/" + cle;
    }

    @Override
    public String urlPresignee(String bucketName, String cle) {
        return urlPresignee(bucketName, cle, Duration.ofMinutes(5));
    }

    @Override
    public String urlPresignee(String bucketName, String cle, Duration duree) {
        return cle == null || cle.isBlank() ? null
                : URL + bucketName + "/" + cle + "?X-Amz-Expires=" + duree.toSeconds() + "&X-Amz-Signature=test";
    }

    @Override
    public String envoyerImage(PhotoPreparee photo, String bucketName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public PhotoStockee envoyerPhotoAvecVignette(PhotoPreparee photo, String bucketName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String uploadPdf(byte[] pdf, String originalName, String bucketName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public byte[] telecharger(String bucketName, String cle) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String uploadImagePrivee(MultipartFile file, String bucketName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void supprimer(String bucketName, String cle) {
        throw new UnsupportedOperationException();
    }
}
