package com.kupanga.api.immobilier.service.impl;

import com.kupanga.api.immobilier.entity.Bien;
import com.kupanga.api.immobilier.entity.BienImage;
import com.kupanga.api.immobilier.repository.BienImageRepository;
import com.kupanga.api.immobilier.service.BienImageService;
import com.kupanga.api.minio.service.MinioService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
@Service
@RequiredArgsConstructor
public class BienImageServiceImpl implements BienImageService {

    private final BienImageRepository bienImageRepository;
    private final MinioService minioService ;

    @Override
    public void uploadImagesImo(List<MultipartFile> files, String bucketName,  Bien bien) {

        List<BienImage> images = files.stream()
                .map(file -> BienImage.builder()
                        .url(minioService.uploadImage(file, bucketName) + bien.getTitre())
                        .bien(bien)
                        .build())
                .toList();

        bienImageRepository.saveAll(images);

    }
}
