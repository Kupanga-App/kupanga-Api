package com.kupanga.api.config.service.impl;

import com.kupanga.api.config.service.MinioService;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

import static com.kupanga.api.config.Constant.ConfigConstant.URL_MINO;

@Service
public class MinioServiceImpl implements MinioService {

    private final MinioClient minioClient;
    private final String bucketAvatarProfil;

    public MinioServiceImpl(MinioClient minioClient,
                        @Value("${minio.bucket.avatar-profil}") String bucketAvatarProfil) {
        this.minioClient = minioClient;
        this.bucketAvatarProfil = bucketAvatarProfil;
    }

    @Override
    public String uploadImage( MultipartFile file){

        try{

            // Génère un nom unique pour éviter les collisions des fichiers
            String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();

            // Envoie du fichier vers MinIO
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketAvatarProfil)
                            .object(fileName)
                            .stream(
                                    file.getInputStream(), // flux du fichier
                                    file.getSize(), // Taille connue
                                    -1 // taille inconnue
                            )
                            .contentType(file.getContentType())// Type MIME (image/png, pdf, etc.)
                            .build()
            );
            // construction de l'Url publique permanente
            return URL_MINO + "/" + bucketAvatarProfil + "/" + fileName;
        } catch (Exception e){
            throw new RuntimeException("Erreur lors de upload vers MinIO" , e);
        }
    }
}
