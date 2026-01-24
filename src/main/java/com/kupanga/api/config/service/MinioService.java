package com.kupanga.api.config.service;

import org.springframework.web.multipart.MultipartFile;

public interface MinioService {

    /**
     * Télécharge une image dans minio
     * @param file chemin d'accès de l'image
     * @return Url de l'image dans minio.
     */
    String uploadImage( MultipartFile file);
}
