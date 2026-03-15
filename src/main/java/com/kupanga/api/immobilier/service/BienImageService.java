package com.kupanga.api.immobilier.service;

import com.kupanga.api.immobilier.entity.Bien;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface BienImageService {

    /**
     * Télécharge les images du bien.
     * @param files liste d'image
     * @param bucketName nom du bucket
     * @param bien le bien.
     */
    void uploadImagesImo(List<MultipartFile> files, String bucketName,  Bien bien);
}
