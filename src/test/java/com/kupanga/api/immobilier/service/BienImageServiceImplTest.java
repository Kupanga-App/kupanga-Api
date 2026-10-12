package com.kupanga.api.immobilier.service;

import com.kupanga.api.juridiction.Pays;
import com.kupanga.api.immobilier.entity.Bien;
import com.kupanga.api.immobilier.entity.BienImage;
import com.kupanga.api.immobilier.repository.BienImageRepository;
import com.kupanga.api.immobilier.service.impl.BienImageServiceImpl;
import com.kupanga.api.minio.image.PhotoPreparee;
import com.kupanga.api.minio.image.PhotoStockee;
import com.kupanga.api.minio.service.MinioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import com.kupanga.api.minio.image.FichierImage;
import com.kupanga.api.minio.image.FormatImage;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@DisplayName("Tests unitaires — BienImageServiceImpl")
class BienImageServiceImplTest {

    @Mock private BienImageRepository bienImageRepository;
    @Mock private MinioService        minioService;

    @InjectMocks
    private BienImageServiceImpl bienImageService;

    private Bien bien;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        bien = Bien.builder().pays(Pays.FR).id(1L).build();
    }

    @Test
    @DisplayName("uploadImagesImo() — 2 fichiers → MinIO appelé 2x, saveAll avec 2 BienImage")
    @SuppressWarnings("unchecked")
    void uploadImagesImo_twoFiles_uploadsAndSaves() {
        PhotoPreparee file1 = new PhotoPreparee(List.of(new FichierImage(new byte[]{1}, FormatImage.JPEG)));
        PhotoPreparee file2 = new PhotoPreparee(List.of(new FichierImage(new byte[]{2}, FormatImage.JPEG)));

        when(minioService.envoyerPhotoAvecVignette(file1, "bucket-test"))
                .thenReturn(new PhotoStockee("http://minio/img1.jpg", "http://minio/vig1.jpg"));
        when(minioService.envoyerPhotoAvecVignette(file2, "bucket-test"))
                .thenReturn(new PhotoStockee("http://minio/img2.jpg", "http://minio/vig2.jpg"));

        bienImageService.uploadImagesImo(List.of(file1, file2), "bucket-test", bien);

        ArgumentCaptor<List<BienImage>> captor = ArgumentCaptor.forClass(List.class);
        verify(bienImageRepository).saveAll(captor.capture());

        List<BienImage> saved = captor.getValue();
        assertThat(saved).hasSize(2);
        assertThat(saved).extracting(BienImage::getUrl)
                .containsExactlyInAnyOrder("http://minio/img1.jpg", "http://minio/img2.jpg");
        // R1 : vignette de 400 px enregistrée avec chaque photo
        assertThat(saved).extracting(BienImage::getUrlVignette)
                .containsExactlyInAnyOrder("http://minio/vig1.jpg", "http://minio/vig2.jpg");
        assertThat(saved).extracting(BienImage::getBien).containsOnly(bien);
    }

    @Test
    @DisplayName("uploadImagesImo() — liste vide → MinIO jamais appelé, saveAll avec liste vide")
    @SuppressWarnings("unchecked")
    void uploadImagesImo_emptyList_noUpload() {
        bienImageService.uploadImagesImo(Collections.emptyList(), "bucket-test", bien);

        verify(minioService, never()).envoyerPhotoAvecVignette(any(), any());

        ArgumentCaptor<List<BienImage>> captor = ArgumentCaptor.forClass(List.class);
        verify(bienImageRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).isEmpty();
    }
}
