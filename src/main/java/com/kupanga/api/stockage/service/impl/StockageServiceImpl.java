package com.kupanga.api.stockage.service.impl;

import com.kupanga.api.stockage.config.StockageProperties;
import com.kupanga.api.stockage.image.CompressionImage;
import com.kupanga.api.stockage.image.FichierImage;
import com.kupanga.api.stockage.image.PhotoPreparee;
import com.kupanga.api.stockage.image.PhotoStockee;
import com.kupanga.api.stockage.service.StockageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.time.Duration;
import java.util.UUID;

import static com.kupanga.api.stockage.constant.StockageConstant.DUREE_URL_PRESIGNEE_MINUTES;

/**
 * S1 : stockage sur tout serveur compatible S3 (SDK AWS v2). Les buckets et leurs droits sont préparés par
 * {@link PreparationBuckets} ; aucun envoi ne modifie la politique d'un bucket.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockageServiceImpl implements StockageService {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final StockageProperties proprietes;
    private final PreparationBuckets preparationBuckets;

    @Override
    public String envoyerImage(PhotoPreparee photo, String bucketName) {
        try {
            return envoyer(bucketName, photo.versions().get(0));
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de l'envoi vers le stockage", e);
        }
    }

    @Override
    public PhotoStockee envoyerPhotoAvecVignette(PhotoPreparee photo, String bucketName) {
        try {
            FichierImage grande = photo.versions().get(0);
            FichierImage vignette = photo.versions().get(1);
            String cleGrande = envoyer(bucketName, grande);
            // Format non réduit (WEBP) : une seule copie, servie aussi comme vignette
            String cleVignette = vignette == grande ? cleGrande : envoyer(bucketName, vignette);
            return new PhotoStockee(cleGrande, cleVignette);
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de l'envoi vers le stockage", e);
        }
    }

    /** @return le nom de l'objet créé */
    private String envoyer(String bucketName, FichierImage version) {
        String nom = UUID.randomUUID() + "." + version.format().getExtension();
        mettre(bucketName, nom, version.contenu(), version.format().getTypeMime());
        return nom;
    }

    private void mettre(String bucketName, String cle, byte[] contenu, String typeMime) {
        preparationBuckets.assurerEcriture(bucketName);
        s3Client.putObject(PutObjectRequest.builder()
                        .bucket(bucketName)
                        .key(cle)
                        .contentType(typeMime)
                        .contentLength((long) contenu.length)
                        .build(),
                RequestBody.fromBytes(contenu));
    }

    @Override
    public String urlPublique(String bucketName, String cle) {
        return cle == null || cle.isBlank() ? null : proprietes.urlObjet(bucketName, cle);
    }

    @Override
    public String uploadPdf(byte[] pdf, String originalName, String bucketName) {
        String fileName = UUID.randomUUID() + "_" + (originalName != null ?
                originalName.replaceAll("\\s+", "") : "file");
        try {
            mettre(bucketName, fileName, pdf, "application/pdf");
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de l'envoi du PDF vers le stockage", e);
        }
        // On stocke la clé, jamais une URL : l'accès passe par une URL présignée (P0-7)
        return fileName;
    }

    @Override
    public String urlPresignee(String bucketName, String cle) {
        return urlPresignee(bucketName, cle, Duration.ofMinutes(DUREE_URL_PRESIGNEE_MINUTES));
    }

    @Override
    public String urlPresignee(String bucketName, String cle, Duration duree) {
        if (cle == null || cle.isBlank()) return null;
        try {
            return s3Presigner.presignGetObject(GetObjectPresignRequest.builder()
                            .signatureDuration(duree)
                            .getObjectRequest(GetObjectRequest.builder().bucket(bucketName).key(cle).build())
                            .build())
                    .url()
                    .toString();
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la génération d'une URL présignée", e);
        }
    }

    @Override
    public byte[] telecharger(String bucketName, String cle) {
        try {
            return s3Client.getObjectAsBytes(GetObjectRequest.builder().bucket(bucketName).key(cle).build()).asByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la lecture dans le stockage", e);
        }
    }

    @Override
    public String uploadImagePrivee(MultipartFile file, String bucketName) {
        // R1 : reçu réduit à 1600 px et sans EXIF/GPS (lisible, mais plus léger sur réseau mobile)
        PhotoPreparee photo = PhotoPreparee.pourBucketPrive(file, CompressionImage.COTE_GRANDE);
        try {
            return envoyer(bucketName, photo.versions().get(0));
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de l'envoi vers le stockage", e);
        }
    }

    @Override
    public void supprimer(String bucketName, String cle) {
        if (cle == null || cle.isBlank()) return;
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucketName).key(cle).build());
        } catch (Exception e) {
            log.warn("Suppression d'un objet du bucket {} impossible : {}", bucketName, e.getClass().getName());
        }
    }
}
