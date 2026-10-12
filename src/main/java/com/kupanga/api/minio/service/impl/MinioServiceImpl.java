package com.kupanga.api.minio.service.impl;

import com.kupanga.api.minio.image.CompressionImage;
import com.kupanga.api.minio.image.FichierImage;
import com.kupanga.api.minio.image.PhotoPreparee;
import com.kupanga.api.minio.image.PhotoStockee;
import com.kupanga.api.minio.service.MinioService;
import io.minio.*;
import io.minio.http.Method;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import static com.kupanga.api.minio.constant.MinioConstant.BUCKETS_PRIVES;
import static com.kupanga.api.minio.constant.MinioConstant.DUREE_URL_PRESIGNEE_MINUTES;

@Slf4j
@Service
public class MinioServiceImpl implements MinioService {

    private final MinioClient minioClient;

    private final MinioClient minioPresignClient;

    private final String url_minio;

    public MinioServiceImpl(MinioClient minioClient ,
                            @Qualifier("minioPresignClient") MinioClient minioPresignClient,
                            @Value("${app.url-mino}")String url_minio ) {

        this.minioClient = minioClient;
        this.minioPresignClient = minioPresignClient;
        this.url_minio = url_minio;
    }

    /**
     * Au démarrage, retire toute politique publique des buckets de documents (contrats, EDL, quittances),
     * y compris sur les buckets créés publics avant P0-7. Ne bloque pas le démarrage si MinIO est indisponible.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void rendreBucketsPrives() {
        for (String bucket : BUCKETS_PRIVES) {
            try {
                createBucketIfNotExists(bucket, false);
            } catch (Exception e) {
                log.warn("Impossible de rendre le bucket {} privé au démarrage : {}", bucket, e.getMessage());
            }
        }
    }

    @Override
    public void createBucketIfNotExists(String bucketName, boolean publicRead) {
        try {
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            }

            if (!publicRead) {
                // Bucket privé : supprime une éventuelle politique publique existante
                minioClient.deleteBucketPolicy(DeleteBucketPolicyArgs.builder().bucket(bucketName).build());
            } else {
                String policyJson = "{\n" +
                        "  \"Version\": \"2012-10-17\",\n" +
                        "  \"Statement\": [\n" +
                        "    {\n" +
                        "      \"Effect\": \"Allow\",\n" +
                        "      \"Principal\": \"*\",\n" +
                        "      \"Action\": [\"s3:GetObject\"],\n" +
                        "      \"Resource\": [\"arn:aws:s3:::" + bucketName + "/*\"]\n" +
                        "    }\n" +
                        "  ]\n" +
                        "}";
                minioClient.setBucketPolicy(
                        SetBucketPolicyArgs.builder()
                                .bucket(bucketName)
                                .config(policyJson)
                                .build()
                );
            }
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la création du bucket MinIO: " + bucketName, e);
        }
    }

    @Override
    public String envoyerImagePublique(PhotoPreparee photo, String bucketName) {
        try {
            createBucketIfNotExists(bucketName, true);
            return url(bucketName, envoyer(bucketName, photo.versions().get(0)));
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de upload vers MinIO", e);
        }
    }

    @Override
    public PhotoStockee envoyerPhotoAvecVignette(PhotoPreparee photo, String bucketName) {
        try {
            createBucketIfNotExists(bucketName, true);
            FichierImage grande = photo.versions().get(0);
            FichierImage vignette = photo.versions().get(1);
            String urlGrande = url(bucketName, envoyer(bucketName, grande));
            // Format non réduit (WEBP) : une seule copie, servie aussi comme vignette
            String urlVignette = vignette == grande ? urlGrande : url(bucketName, envoyer(bucketName, vignette));
            return new PhotoStockee(urlGrande, urlVignette);
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de upload vers MinIO", e);
        }
    }

    /** @return le nom de l'objet créé */
    private String envoyer(String bucketName, FichierImage version) throws Exception {
        String nom = UUID.randomUUID() + "." + version.format().getExtension();
        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(bucketName)
                        .object(nom)
                        .stream(new ByteArrayInputStream(version.contenu()), version.contenu().length, -1)
                        .contentType(version.format().getTypeMime())
                        .build()
        );
        return nom;
    }

    private String url(String bucketName, String nom) {
        return url_minio + "/" + bucketName + "/" + nom;
    }

    /** Nom d'objet simple : ni sous-dossier, ni « .. », ni paramètres. */
    private static final Pattern NOM_OBJET = Pattern.compile("[A-Za-z0-9][A-Za-z0-9_-]*(\\.[A-Za-z0-9]+)?");

    @Override
    public boolean estUrlDuBucket(String url, String bucketName) {
        String prefixe = url_minio + "/" + bucketName + "/";
        return url != null
                && url.startsWith(prefixe)
                && NOM_OBJET.matcher(url.substring(prefixe.length())).matches();
    }

    @Override
    public String uploadPdf(byte[] pdf, String originalName ,String bucketName ) {

        try {

            // Crée le bucket si nécessaire
            createBucketIfNotExists(bucketName, false); // privé : données personnelles (P0-7)

            String fileName = UUID.randomUUID() + "_" + (originalName != null ?
                    originalName.replaceAll("\\s+", "") : "file");

            // Envoie le PDF sur le serveur MinIO
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)          // Nom du bucket où le fichier sera stocké
                            .object(fileName)               // Nom du fichier dans le bucket
                            .stream(
                                    new ByteArrayInputStream(pdf), // Convertit le tableau d'octets en InputStream
                                    pdf.length,                     // Taille du PDF
                                    -1                               // Taille inconnue du stream (-1 si connue)
                            )
                            .contentType("application/pdf")  // Définit le type MIME du fichier
                            .build()
            );

            // On stocke la clé, jamais une URL : l'accès passe par une URL présignée
            return fileName;

        } catch (Exception e) {
            throw new RuntimeException("Erreur upload PDF MinIO", e);
        }
    }

    @Override
    public String urlPresignee(String bucketName, String cle) {
        if (cle == null || cle.isBlank()) return null;
        try {
            return minioPresignClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucketName)
                            .object(cle)
                            .expiry(DUREE_URL_PRESIGNEE_MINUTES, TimeUnit.MINUTES)
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("Erreur génération URL présignée MinIO", e);
        }
    }

    @Override
    public byte[] telecharger(String bucketName, String cle) {
        try (InputStream in = minioClient.getObject(
                GetObjectArgs.builder().bucket(bucketName).object(cle).build())) {
            return in.readAllBytes();
        } catch (Exception e) {
            throw new RuntimeException("Erreur téléchargement MinIO", e);
        }
    }

    @Override
    public void supprimerParUrl(String url, String bucketName) {
        if (!estUrlDuBucket(url, bucketName)) {
            return;
        }
        String objet = url.substring((url_minio + "/" + bucketName + "/").length());
        try {
            minioClient.removeObject(RemoveObjectArgs.builder().bucket(bucketName).object(objet).build());
        } catch (Exception e) {
            log.warn("Suppression d'un objet du bucket {} impossible : {}", bucketName, e.getClass().getName());
        }
    }

    @Override
    public String uploadImagePrivee(MultipartFile file, String bucketName) {
        // R1 : reçu réduit à 1600 px et sans EXIF/GPS (lisible, mais plus léger sur réseau mobile)
        PhotoPreparee photo = PhotoPreparee.pourBucketPrive(file, CompressionImage.COTE_GRANDE);
        try {
            createBucketIfNotExists(bucketName, false);
            return envoyer(bucketName, photo.versions().get(0));
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de upload vers MinIO", e);
        }
    }

    @Override
    public void supprimer(String bucketName, String cle) {
        if (cle == null || cle.isBlank()) return;
        try {
            minioClient.removeObject(RemoveObjectArgs.builder().bucket(bucketName).object(cle).build());
        } catch (Exception e) {
            log.warn("Suppression d'un objet du bucket {} impossible : {}", bucketName, e.getClass().getName());
        }
    }
}
