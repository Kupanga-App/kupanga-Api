package com.kupanga.api.minio.service;

import com.kupanga.api.minio.service.impl.MinioServiceImpl;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.SetBucketPolicyArgs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import static com.kupanga.api.minio.constant.MinioConstant.URL_MINIO;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("Tests unitaires pour MinioServiceImpl (générique)")
class MinioServiceImplTest {

    private MinioClient minioClient;
    private MinioServiceImpl minioService;

    @BeforeEach
    void setUp() {
        minioClient = mock(MinioClient.class);
        minioService = new MinioServiceImpl(minioClient);
    }

    // =======================
    // Tests pour uploadFile
    // =======================

    @Test
    @DisplayName("Upload d'image : l'URL retournée contient le bucket et le nom du fichier")
    void uploadFile_shouldReturnUrl() throws Exception {
        String bucket = "avatars";
        MultipartFile file = new MockMultipartFile(
                "file",
                "test.png",
                "image/png",
                "dummy content".getBytes()
        );

        // Simule que le bucket existe
        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);

        String result = minioService.uploadImage(file, bucket);

        assertNotNull(result, "L'URL ne doit pas être null");
        assertTrue(result.startsWith(URL_MINIO + "/" + bucket + "/"), "L'URL doit contenir le bucket");
        assertTrue(result.endsWith("_test.png"), "L'URL doit contenir le nom du fichier original");

        verify(minioClient, times(1)).putObject(any(PutObjectArgs.class));
    }

    @Test
    @DisplayName("Vérifie que putObject est appelé avec les bons arguments")
    void uploadFile_shouldCallPutObjectWithCorrectArgs() throws Exception {
        String bucket = "avatars";
        MultipartFile file = new MockMultipartFile(
                "file",
                "myphoto.jpg",
                "image/jpeg",
                "dummy".getBytes()
        );

        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);

        minioService.uploadImage(file, bucket);

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
    void uploadFile_shouldThrowRuntimeException_whenMinioFails() throws Exception {
        String bucket = "avatars";
        MultipartFile file = new MockMultipartFile(
                "file",
                "fail.png",
                "image/png",
                "data".getBytes()
        );

        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);
        doThrow(new RuntimeException("MinIO error")).when(minioClient).putObject(any(PutObjectArgs.class));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> minioService.uploadImage(file, bucket));

        assertTrue(exception.getMessage().toLowerCase().contains("minio"));
    }

    @Test
    @DisplayName("Upload avec nom de fichier vide : l'URL retournée est toujours correcte")
    void uploadFile_shouldHandleEmptyOriginalFilename() throws Exception {
        String bucket = "avatars";
        MultipartFile file = new MockMultipartFile(
                "file",
                "",
                "image/png",
                "data".getBytes()
        );

        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);

        String result = minioService.uploadImage(file, bucket);

        assertNotNull(result, "L'URL ne doit pas être null même si le nom du fichier est vide");
        assertTrue(result.startsWith(URL_MINIO + "/" + bucket + "/"), "L'URL doit contenir le bucket");
    }

    // =======================
    // Tests pour createBucketIfNotExists
    // =======================

    @Test
    @DisplayName("Doit créer un bucket s'il n'existe pas et appliquer la politique publique")
    void createBucketIfNotExists_shouldCreateBucketAndSetPolicy() throws Exception {
        String bucket = "new-bucket";

        // Simule que le bucket n'existe pas
        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(false);

        minioService.createBucketIfNotExists(bucket, true);

        // Vérifie que makeBucket et setBucketPolicy ont été appelés
        verify(minioClient, times(1)).makeBucket(any(MakeBucketArgs.class));
        verify(minioClient, times(1)).setBucketPolicy(any(SetBucketPolicyArgs.class));
    }

    @Test
    @DisplayName("Ne crée pas le bucket s'il existe déjà mais applique la politique publique")
    void createBucketIfNotExists_shouldOnlySetPolicyIfBucketExists() throws Exception {
        String bucket = "existing-bucket";

        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);

        minioService.createBucketIfNotExists(bucket, true);

        // makeBucket ne doit pas être appelé
        verify(minioClient, never()).makeBucket(any(MakeBucketArgs.class));
        // setBucketPolicy doit être appelé
        verify(minioClient, times(1)).setBucketPolicy(any(SetBucketPolicyArgs.class));
    }

    @Test
    @DisplayName("Doit lever RuntimeException si la création ou la politique échoue")
    void createBucketIfNotExists_shouldThrowRuntimeExceptionOnFailure() throws Exception {
        String bucket = "fail-bucket";

        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenThrow(new RuntimeException("MinIO error"));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> minioService.createBucketIfNotExists(bucket, true));

        assertTrue(exception.getMessage().contains("Erreur lors de la création du bucket MinIO"));
    }
}
