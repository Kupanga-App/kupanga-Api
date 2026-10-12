package com.kupanga.api.immobilier.service.impl;

import com.kupanga.api.immobilier.entity.Bien;
import com.kupanga.api.immobilier.entity.BienImage;
import com.kupanga.api.immobilier.repository.BienImageRepository;
import com.kupanga.api.immobilier.service.BienImageService;
import com.kupanga.api.minio.service.MinioService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.kupanga.api.minio.image.PhotoPreparee;

import java.util.List;
@Service
@RequiredArgsConstructor
public class BienImageServiceImpl implements BienImageService {

    private final BienImageRepository bienImageRepository;
    private final MinioService minioService ;

    @Override
    public void uploadImagesImo(List<PhotoPreparee> photos, String bucketName,  Bien bien) {

        // R1 : version de 1600 px + vignette de 400 px, en JPEG sans EXIF
        List<BienImage> images = photos.stream()
                .map(photo -> minioService.envoyerPhotoAvecVignette(photo, bucketName))
                .map(photo -> BienImage.builder()
                        .url(photo.url())
                        .urlVignette(photo.urlVignette())
                        .bien(bien)
                        .build())
                .toList();

        bienImageRepository.saveAll(images);

    }
}
