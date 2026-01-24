package com.kupanga.api.config.service;

import com.kupanga.api.config.service.impl.MinioServiceImpl;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import static com.kupanga.api.config.Constant.ConfigConstant.URL_MINO;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("Tests unitaires pour MinioServiceImpl")
class MinioServiceImplTest {

    private MinioClient minioClient;
    private MinioServiceImpl minioService;
    private final String bucket = "avatars";

    @BeforeEach
    void setUp() {
        minioClient = mock(MinioClient.class);
        minioService = new MinioServiceImpl(minioClient, bucket);
    }

    @Test
    @DisplayName("Upload d'image : l'URL retournée contient le bucket et le nom du fichier")
    void uploadImage_shouldReturnUrl() throws Exception {
        MultipartFile file = new MockMultipartFile(
                "file",
                "test.png",
                "image/png",
                "dummy content".getBytes()
        );

        String result = minioService.uploadImage(file);

        assertNotNull(result, "L'URL ne doit pas être null");
        assertTrue(result.startsWith(URL_MINO + "/" + bucket + "/"), "L'URL doit contenir le bucket");
        assertTrue(result.endsWith("_test.png"), "L'URL doit contenir le nom du fichier original");

        verify(minioClient, times(1)).putObject(any(PutObjectArgs.class));
    }

    @Test
    @DisplayName("Vérifie que putObject est appelé avec les bons arguments")
    void uploadImage_shouldCallPutObjectWithCorrectArgs() throws Exception {
        MultipartFile file = new MockMultipartFile(
                "file",
                "myphoto.jpg",
                "image/jpeg",
                "dummy".getBytes()
        );

        minioService.uploadImage(file);

        ArgumentCaptor<PutObjectArgs> captor = ArgumentCaptor.forClass(PutObjectArgs.class);
        verify(minioClient).putObject(captor.capture());

        PutObjectArgs args = captor.getValue();
        assertEquals(bucket, args.bucket(), "Le bucket doit correspondre à celui configuré");
        assertEquals("myphoto.jpg", args.object().substring(args.object().indexOf("_") + 1),
                "Le nom de l'objet doit correspondre au nom du fichier original");
        assertEquals(file.getContentType(), args.contentType(), "Le type MIME doit correspondre au fichier");
    }

    @Test
    @DisplayName("Doit lever une RuntimeException si MinIO échoue lors de l'upload")
    void uploadImage_shouldThrowRuntimeException_whenMinioFails() throws Exception {
        MultipartFile file = new MockMultipartFile(
                "file",
                "fail.png",
                "image/png",
                "data".getBytes()
        );

        doThrow(new RuntimeException("MinIO error")).when(minioClient).putObject(any(PutObjectArgs.class));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> minioService.uploadImage(file));

        assertTrue(exception.getMessage().contains("Erreur lors de upload vers MinIO"));
    }

    @Test
    @DisplayName("Upload avec nom de fichier vide : l'URL retournée est toujours correcte")
    void uploadImage_shouldHandleEmptyOriginalFilename() throws Exception {
        MultipartFile file = new MockMultipartFile(
                "file",
                "",
                "image/png",
                "data".getBytes()
        );

        String result = minioService.uploadImage(file);

        assertNotNull(result, "L'URL ne doit pas être null même si le nom du fichier est vide");
        assertTrue(result.startsWith(URL_MINO + "/" + bucket + "/"), "L'URL doit contenir le bucket");
    }
}
