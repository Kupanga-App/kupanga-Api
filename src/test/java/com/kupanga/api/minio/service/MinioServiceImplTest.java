package com.kupanga.api.minio.service;

import com.kupanga.api.minio.service.impl.MinioServiceImpl;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.SetBucketPolicyArgs;
import io.minio.DeleteBucketPolicyArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.http.Method;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import com.kupanga.api.exception.business.KupangaBusinessException;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("Tests unitaires pour MinioServiceImpl (générique)")
class MinioServiceImplTest {

    private MinioClient minioClient;
    private MinioClient minioPresignClient;
    private MinioServiceImpl minioService;

    @BeforeEach
    void setUp() {
        minioClient = mock(MinioClient.class);
        minioPresignClient = mock(MinioClient.class);
        minioService = new MinioServiceImpl(minioClient , minioPresignClient, "http://localhost:9000");
    }

    // =======================
    // P0-7 : PDF privés
    // =======================

    @Test
    @DisplayName("uploadPdf : bucket rendu privé (politique publique supprimée), renvoie une clé et pas une URL")
    void uploadPdf_shouldUsePrivateBucketAndReturnKey() throws Exception {
        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);

        String cle = minioService.uploadPdf("pdf".getBytes(), "contrat_1.pdf", "contrat-de-bail");

        assertFalse(cle.startsWith("http"), "On stocke une clé, jamais une URL");
        assertTrue(cle.endsWith("_contrat_1.pdf"));
        verify(minioClient).deleteBucketPolicy(any(DeleteBucketPolicyArgs.class));
        verify(minioClient, never()).setBucketPolicy(any(SetBucketPolicyArgs.class));
    }

    @Test
    @DisplayName("urlPresignee : URL GET signée par le client public, valable 5 minutes")
    void urlPresignee_shouldSignWithPublicClientFor5Minutes() throws Exception {
        when(minioPresignClient.getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class)))
                .thenReturn("http://localhost:9000/contrat-de-bail/cle.pdf?X-Amz-Signature=abc");

        String url = minioService.urlPresignee("contrat-de-bail", "cle.pdf");

        ArgumentCaptor<GetPresignedObjectUrlArgs> captor = ArgumentCaptor.forClass(GetPresignedObjectUrlArgs.class);
        verify(minioPresignClient).getPresignedObjectUrl(captor.capture());
        assertEquals(5 * 60, captor.getValue().expiry());
        assertEquals(Method.GET, captor.getValue().method());
        assertEquals("cle.pdf", captor.getValue().object());
        assertTrue(url.contains("X-Amz-Signature"));
    }

    @Test
    @DisplayName("urlPresignee : clé vide → null, aucun appel MinIO")
    void urlPresignee_nullKey_returnsNull() {
        assertNull(minioService.urlPresignee("contrat-de-bail", null));
        verifyNoInteractions(minioPresignClient);
    }

    // =======================
    // Tests pour uploadFile
    // =======================

    /** En-têtes réels (B5 : le format est reconnu au contenu). */
    private static final byte[] CONTENU_PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0x0D};
    private static final byte[] CONTENU_JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0x10};

    @Test
    @DisplayName("Upload d'image : l'URL retournée contient le bucket et le nom du fichier")
    void uploadFile_shouldReturnUrl() throws Exception {
        String bucket = "avatars";
        MultipartFile file = new MockMultipartFile(
                "file",
                "test.png",
                "image/png",
                CONTENU_PNG
        );

        // Simule que le bucket existe
        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);

        String result = minioService.uploadImage(file, bucket);

        assertNotNull(result, "L'URL ne doit pas être null");
        assertTrue(result.startsWith("http://localhost:9000" + "/" + bucket + "/"), "L'URL doit contenir le bucket");
        assertTrue(result.endsWith(".png"), "Extension déduite du contenu");
        assertFalse(result.contains("test"), "B5 : le nom d'origine n'est plus repris");

        verify(minioClient, times(1)).putObject(any(PutObjectArgs.class));
    }

    @Test
    @DisplayName("B5 : nom et type MIME fixés par le serveur d'après le contenu, pas d'après le client")
    void uploadFile_shouldCallPutObjectWithCorrectArgs() throws Exception {
        String bucket = "avatars";
        // Le client annonce un JPEG nommé .jpg, le contenu est un PNG
        MultipartFile file = new MockMultipartFile(
                "file",
                "../my photo.jpg",
                "image/jpeg",
                CONTENU_PNG
        );

        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);

        minioService.uploadImage(file, bucket);

        ArgumentCaptor<PutObjectArgs> captor = ArgumentCaptor.forClass(PutObjectArgs.class);
        verify(minioClient).putObject(captor.capture());

        PutObjectArgs args = captor.getValue();
        assertEquals(bucket, args.bucket(), "Le bucket doit correspondre à celui configuré");
        assertTrue(args.object().matches("[0-9a-f-]{36}\\.png"), "Nom généré : UUID + extension réelle");
        assertEquals("image/png", args.contentType(), "Type MIME déduit du contenu");
    }

    @Test
    @DisplayName("Doit lever une RuntimeException si MinIO échoue lors de l'upload")
    void uploadFile_shouldThrowRuntimeException_whenMinioFails() throws Exception {
        String bucket = "avatars";
        MultipartFile file = new MockMultipartFile(
                "file",
                "fail.png",
                "image/png",
                CONTENU_PNG
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
                CONTENU_JPEG
        );

        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);

        String result = minioService.uploadImage(file, bucket);

        assertNotNull(result, "L'URL ne doit pas être null même si le nom du fichier est vide");
        assertTrue(result.startsWith("http://localhost:9000" + "/" + bucket + "/"), "L'URL doit contenir le bucket");
    }

    @Test
    @DisplayName("B5 : page HTML déguisée en photo (.jpg, image/jpeg) → 415, rien n'est envoyé à MinIO")
    void uploadFile_contenuNonImage_refuse() throws Exception {
        MultipartFile piege = new MockMultipartFile(
                "file", "photo.jpg", "image/jpeg", "<html><script>alert(1)</script></html>".getBytes());

        KupangaBusinessException e = assertThrows(KupangaBusinessException.class,
                () -> minioService.uploadImage(piege, "avatars"));

        assertEquals(415, e.getStatus().value());
        verify(minioClient, never()).putObject(any(PutObjectArgs.class));
        verify(minioClient, never()).makeBucket(any(MakeBucketArgs.class));
    }

    @Test
    @DisplayName("estUrlDuBucket : seules les URL d'un objet simple de ce bucket sur notre MinIO sont acceptées")
    void estUrlDuBucket() {
        String bucket = "bucket-photo-profil";
        assertTrue(minioService.estUrlDuBucket("http://localhost:9000/bucket-photo-profil/avatar-3.png", bucket));
        assertTrue(minioService.estUrlDuBucket("http://localhost:9000/bucket-photo-profil/2f1c-uuid.jpg", bucket));

        assertFalse(minioService.estUrlDuBucket(null, bucket));
        assertFalse(minioService.estUrlDuBucket("https://traqueur.example/pixel.gif", bucket));
        assertFalse(minioService.estUrlDuBucket("http://localhost:9000.traqueur.example/bucket-photo-profil/a.png", bucket));
        assertFalse(minioService.estUrlDuBucket("http://localhost:9000/photos-imo/a.png", bucket));
        assertFalse(minioService.estUrlDuBucket("http://localhost:9000/bucket-photo-profil/../contrat-de-bail/x.pdf", bucket));
        assertFalse(minioService.estUrlDuBucket("http://localhost:9000/bucket-photo-profil/a.png?response-content-type=text/html", bucket));
        assertFalse(minioService.estUrlDuBucket("javascript:alert(1)", bucket));
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
