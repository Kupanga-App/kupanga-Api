package com.kupanga.api.minio.service.impl;

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
    public String uploadImage(MultipartFile file, String bucketName) {
        try {
            // Crée le bucket si nécessaire
            createBucketIfNotExists(bucketName, true);
            String originalName = file.getOriginalFilename();

            String fileName = UUID.randomUUID() + "_" + (originalName != null ?
                    originalName.replaceAll("\\s+", "") : "file");

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fileName)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );

            return url_minio + "/" + bucketName + "/" + fileName;
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de upload vers MinIO", e);
        }
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
}
