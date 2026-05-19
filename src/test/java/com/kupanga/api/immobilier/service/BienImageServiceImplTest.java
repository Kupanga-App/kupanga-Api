package com.kupanga.api.immobilier.service;

import com.kupanga.api.immobilier.entity.Bien;
import com.kupanga.api.immobilier.entity.BienImage;
import com.kupanga.api.immobilier.repository.BienImageRepository;
import com.kupanga.api.immobilier.service.impl.BienImageServiceImpl;
import com.kupanga.api.minio.service.MinioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.web.multipart.MultipartFile;

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
        bien = Bien.builder().id(1L).build();
    }

    @Test
    @DisplayName("uploadImagesImo() — 2 fichiers → MinIO appelé 2x, saveAll avec 2 BienImage")
    @SuppressWarnings("unchecked")
    void uploadImagesImo_twoFiles_uploadsAndSaves() {
        MultipartFile file1 = mock(MultipartFile.class);
        MultipartFile file2 = mock(MultipartFile.class);

        when(minioService.uploadImage(file1, "bucket-test")).thenReturn("http://minio/img1.jpg");
        when(minioService.uploadImage(file2, "bucket-test")).thenReturn("http://minio/img2.jpg");

        bienImageService.uploadImagesImo(List.of(file1, file2), "bucket-test", bien);

        ArgumentCaptor<List<BienImage>> captor = ArgumentCaptor.forClass(List.class);
        verify(bienImageRepository).saveAll(captor.capture());

        List<BienImage> saved = captor.getValue();
        assertThat(saved).hasSize(2);
        assertThat(saved).extracting(BienImage::getUrl)
                .containsExactlyInAnyOrder("http://minio/img1.jpg", "http://minio/img2.jpg");
        assertThat(saved).extracting(BienImage::getBien).containsOnly(bien);
    }

    @Test
    @DisplayName("uploadImagesImo() — liste vide → MinIO jamais appelé, saveAll avec liste vide")
    @SuppressWarnings("unchecked")
    void uploadImagesImo_emptyList_noUpload() {
        bienImageService.uploadImagesImo(Collections.emptyList(), "bucket-test", bien);

        verify(minioService, never()).uploadImage(any(), any());

        ArgumentCaptor<List<BienImage>> captor = ArgumentCaptor.forClass(List.class);
        verify(bienImageRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).isEmpty();
    }
}
