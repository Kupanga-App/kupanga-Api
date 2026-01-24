package com.kupanga.api.minio.service;

import org.springframework.web.multipart.MultipartFile;

public interface MinioService {

    /**
     * Créer le bucket s'il n'existe pas et l'initialise à public.
     * @param bucketName nom du bucket.
     * @param publicRead booléen le spécifiant public
     */
    void createBucketIfNotExists(String bucketName, boolean publicRead);

    /**
     * Télécharge une image dans minio
     * @param file chemin d'accès de l'image
     * @param bucketName nom du bucket
     * @return Url de l'image dans minio.
     */
    String uploadImage(MultipartFile file, String bucketName);
}
