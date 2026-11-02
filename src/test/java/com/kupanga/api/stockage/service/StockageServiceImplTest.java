package com.kupanga.api.stockage.service;

import com.kupanga.api.exception.business.KupangaBusinessException;
import com.kupanga.api.stockage.config.StockageProperties;
import com.kupanga.api.stockage.image.ImagesDeTest;
import com.kupanga.api.stockage.image.PhotoPreparee;
import com.kupanga.api.stockage.image.PhotoStockee;
import com.kupanga.api.stockage.service.impl.PreparationBuckets;
import com.kupanga.api.stockage.service.impl.StockageServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteBucketPolicyRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutBucketPolicyRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.InputStream;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("Tests unitaires de StockageServiceImpl (S3 générique)")
class StockageServiceImplTest {

    private static final StockageProperties PROPRIETES = new StockageProperties(
            "http://stockage:9000", "http://localhost:9000/", "us-east-1", true, "cle", "secret", true);

    private S3Client s3Client;
    private S3Presigner s3Presigner;
    private PreparationBuckets preparationBuckets;
    private StockageServiceImpl stockageService;

    @BeforeEach
    void setUp() {
        s3Client = mock(S3Client.class);
        s3Presigner = mock(S3Presigner.class);
        preparationBuckets = mock(PreparationBuckets.class);
        stockageService = new StockageServiceImpl(s3Client, s3Presigner, PROPRIETES, preparationBuckets);
    }

    /** Requêtes putObject envoyées, dans l'ordre. */
    private List<PutObjectRequest> requetesEnvoyees() {
        ArgumentCaptor<PutObjectRequest> captor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client, atLeastOnce()).putObject(captor.capture(), any(RequestBody.class));
        return captor.getAllValues();
    }

    /** Contenu envoyé pour chaque putObject, dans l'ordre. */
    private List<byte[]> contenusEnvoyes() throws Exception {
        ArgumentCaptor<RequestBody> captor = ArgumentCaptor.forClass(RequestBody.class);
        verify(s3Client, atLeastOnce()).putObject(any(PutObjectRequest.class), captor.capture());
        List<byte[]> contenus = new ArrayList<>();
        for (RequestBody corps : captor.getAllValues()) {
            try (InputStream in = corps.contentStreamProvider().newStream()) {
                contenus.add(in.readAllBytes());
            }
        }
        return contenus;
    }

    @Test
    @DisplayName("S1 : aucun envoi ne touche à la politique d'un bucket ; les buckets sont préparés avant l'envoi")
    void envoi_sansPolitique() {
        stockageService.uploadPdf("pdf".getBytes(), "contrat_1.pdf", "contrat-de-bail");
        stockageService.envoyerImage(PhotoPreparee.pourBucketPublic(
                new MockMultipartFile("f", "a.png", "image/png", CONTENU_PNG), 400), "avatars");

        verify(preparationBuckets).assurerEcriture("contrat-de-bail");
        verify(preparationBuckets).assurerEcriture("avatars");
        verify(s3Client, never()).putBucketPolicy(any(PutBucketPolicyRequest.class));
        verify(s3Client, never()).deleteBucketPolicy(any(DeleteBucketPolicyRequest.class));
    }

    @Test
    @DisplayName("S1 : stockage injoignable → l'envoi échoue sans rien écrire")
    void envoi_stockageInjoignable() {
        doThrow(new IllegalStateException("injoignable")).when(preparationBuckets).assurerEcriture(any());

        assertThrows(RuntimeException.class,
                () -> stockageService.uploadPdf("pdf".getBytes(), "c.pdf", "contrat-de-bail"));
        verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    // =======================
    // P0-7 : PDF privés
    // =======================

    @Test
    @DisplayName("uploadPdf : PDF envoyé dans le bucket demandé ; renvoie une clé et pas une URL")
    void uploadPdf_renvoieUneCle() {
        String cle = stockageService.uploadPdf("pdf".getBytes(), "contrat 1.pdf", "contrat-de-bail");

        assertFalse(cle.startsWith("http"), "On stocke une clé, jamais une URL");
        assertTrue(cle.endsWith("_contrat1.pdf"));
        PutObjectRequest requete = requetesEnvoyees().get(0);
        assertEquals("contrat-de-bail", requete.bucket());
        assertEquals(cle, requete.key());
        assertEquals("application/pdf", requete.contentType());
    }

    @Test
    @DisplayName("urlPresignee : URL GET signée (client public), valable 5 minutes")
    void urlPresignee_signee5Minutes() throws Exception {
        PresignedGetObjectRequest signee = mock(PresignedGetObjectRequest.class);
        when(signee.url()).thenReturn(URI.create("http://localhost:9000/contrat-de-bail/cle.pdf?X-Amz-Signature=abc").toURL());
        when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class))).thenReturn(signee);

        String url = stockageService.urlPresignee("contrat-de-bail", "cle.pdf");

        ArgumentCaptor<GetObjectPresignRequest> captor = ArgumentCaptor.forClass(GetObjectPresignRequest.class);
        verify(s3Presigner).presignGetObject(captor.capture());
        assertEquals(Duration.ofMinutes(5), captor.getValue().signatureDuration());
        assertEquals("contrat-de-bail", captor.getValue().getObjectRequest().bucket());
        assertEquals("cle.pdf", captor.getValue().getObjectRequest().key());
        assertTrue(url.contains("X-Amz-Signature"));
    }

    @Test
    @DisplayName("urlPresignee : clé vide → null, aucun appel au stockage")
    void urlPresignee_nullKey_returnsNull() {
        assertNull(stockageService.urlPresignee("contrat-de-bail", null));
        assertNull(stockageService.urlPresignee("contrat-de-bail", " "));
        verifyNoInteractions(s3Presigner);
    }

    // =======================
    // Tests pour uploadFile
    // =======================

    /** Vraies images (B5 : format reconnu au contenu ; R1 : pixels décodés et réencodés). */
    private static final byte[] CONTENU_PNG = ImagesDeTest.png(60, 40);
    private static final byte[] CONTENU_JPEG = ImagesDeTest.jpeg(60, 40);

    @Test
    @DisplayName("S2 : envoi d'image → clé de l'objet (jamais une URL)")
    void uploadFile_renvoieUneCle() throws Exception {
        String bucket = "avatars";
        MultipartFile file = new MockMultipartFile(
                "file",
                "test.png",
                "image/png",
                CONTENU_PNG
        );

        String result = stockageService.envoyerImage(PhotoPreparee.pourBucketPublic(file, 400), bucket);

        assertNotNull(result);
        assertTrue(result.matches("[0-9a-f-]{36}\\.jpg"), "S2 : clé UUID ; R1 : réencodé en JPEG");
        assertFalse(result.contains("test"), "B5 : le nom d'origine n'est plus repris");

        verify(s3Client, times(1)).putObject(any(PutObjectRequest.class), any(RequestBody.class));
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

        stockageService.envoyerImage(PhotoPreparee.pourBucketPublic(file, 400), bucket);

        PutObjectRequest args = requetesEnvoyees().get(0);
        assertEquals(bucket, args.bucket(), "Le bucket doit correspondre à celui configuré");
        assertTrue(args.key().matches("[0-9a-f-]{36}\\.jpg"), "Nom généré : UUID + extension du JPEG produit");
        assertEquals("image/jpeg", args.contentType(), "Type MIME fixé par le serveur (R1 : JPEG)");
    }

    @Test
    @DisplayName("Erreur du stockage à l'envoi → RuntimeException")
    void uploadFile_erreurDuStockage() throws Exception {
        String bucket = "avatars";
        MultipartFile file = new MockMultipartFile(
                "file",
                "fail.png",
                "image/png",
                CONTENU_PNG
        );

        doThrow(S3Exception.builder().statusCode(500).build())
                .when(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> stockageService.envoyerImage(PhotoPreparee.pourBucketPublic(file, 400), bucket));

        assertTrue(exception.getMessage().contains("stockage"));
    }

    @Test
    @DisplayName("Upload avec nom de fichier vide : clé toujours générée")
    void uploadFile_shouldHandleEmptyOriginalFilename() throws Exception {
        String bucket = "avatars";
        MultipartFile file = new MockMultipartFile(
                "file",
                "",
                "image/png",
                CONTENU_JPEG
        );

        String result = stockageService.envoyerImage(PhotoPreparee.pourBucketPublic(file, 400), bucket);

        assertNotNull(result, "La clé ne doit pas être null même si le nom du fichier est vide");
        assertFalse(result.startsWith("http"));
    }

    @Test
    @DisplayName("B5 : page HTML déguisée en photo (.jpg, image/jpeg) → 415, rien n'est envoyé au stockage")
    void uploadFile_contenuNonImage_refuse() throws Exception {
        MultipartFile piege = new MockMultipartFile(
                "file", "photo.jpg", "image/jpeg", "<html><script>alert(1)</script></html>".getBytes());

        KupangaBusinessException e = assertThrows(KupangaBusinessException.class,
                () -> stockageService.uploadImagePrivee(piege, "avatars"));

        assertEquals(415, e.getStatus().value());
        verifyNoInteractions(s3Client, preparationBuckets);
    }

    // =======================
    // R1 : compression
    // =======================

    @Test
    @DisplayName("R1 : photo de bien → version de 1600 px et vignette de 400 px, deux JPEG sans EXIF")
    void uploadPhotoAvecVignette_deuxVersions() throws Exception {
        MultipartFile photo = new MockMultipartFile("files", "p.jpg", "image/jpeg",
                ImagesDeTest.jpegAvecExif(3200, 2400, 1, true));

        PhotoStockee stockee = stockageService.envoyerPhotoAvecVignette(PhotoPreparee.pourBucketPublic(photo, 1600, 400), "bucket-photo-immobilier");

        assertTrue(stockee.cle().matches("[0-9a-f-]{36}\\.jpg"), "S2 : clé, pas URL");
        assertTrue(stockee.cleVignette().endsWith(".jpg"));
        assertNotEquals(stockee.cle(), stockee.cleVignette());
        List<byte[]> contenus = contenusEnvoyes();
        assertEquals(2, contenus.size());
        assertEquals(1600, ImagesDeTest.lire(contenus.get(0)).getWidth());
        assertEquals(400, ImagesDeTest.lire(contenus.get(1)).getWidth());
        for (byte[] contenu : contenus) {
            assertFalse(ImagesDeTest.contient(contenu, "GPS-KINSHASA"), "EXIF/GPS retiré");
        }
    }

    @Test
    @DisplayName("R1 : WEBP (non décodable en Java pur) stocké une seule fois, sans EXIF, servi aussi en vignette")
    void uploadPhotoAvecVignette_webpTelQuel() throws Exception {
        byte[] webp = ImagesDeTest.webpAvecExif();
        MultipartFile photo = new MockMultipartFile("files", "p.webp", "image/webp", webp);

        PhotoStockee stockee = stockageService.envoyerPhotoAvecVignette(PhotoPreparee.pourBucketPublic(photo, 1600, 400), "bucket-photo-immobilier");

        assertEquals(stockee.cle(), stockee.cleVignette());
        assertTrue(stockee.cle().endsWith(".webp"));
        assertEquals(1, requetesEnvoyees().size());
        assertEquals("image/webp", requetesEnvoyees().get(0).contentType());
        assertFalse(ImagesDeTest.contient(contenusEnvoyes().get(0), "GPS-KINSHASA"));
    }

    @Test
    @DisplayName("R1 : photo de profil réduite à 400 px")
    void uploadImage_profil400() throws Exception {
        stockageService.envoyerImage(PhotoPreparee.pourBucketPublic(
                new MockMultipartFile("f", "a.png", "image/png", ImagesDeTest.png(1200, 900)), 400), "bucket-photo-profil");

        assertEquals(400, ImagesDeTest.lire(contenusEnvoyes().get(0)).getWidth());
    }

    @Test
    @DisplayName("R1 / C8-bis : reçu réduit à 1600 px, sans EXIF, dans un bucket privé ; renvoie une clé")
    void uploadImagePrivee_recuCompresse() throws Exception {
        MultipartFile recu = new MockMultipartFile("recu", "r.jpg", "image/jpeg",
                ImagesDeTest.jpegAvecExif(2000, 3000, 1, false));

        String cle = stockageService.uploadImagePrivee(recu, "bucket-recus-paiement");

        assertTrue(cle.matches("[0-9a-f-]{36}\\.jpg"));
        byte[] contenu = contenusEnvoyes().get(0);
        assertEquals(1600, ImagesDeTest.lire(contenu).getHeight());
        assertFalse(ImagesDeTest.contient(contenu, "GPS-KINSHASA"));
        assertEquals("bucket-recus-paiement", requetesEnvoyees().get(0).bucket());
    }

    @Test
    @DisplayName("S2 : URL publique construite à la lecture depuis la clé ; clé vide → null")
    void urlPublique() {
        assertEquals("http://localhost:9000/bucket-photo-immobilier/a.jpg",
                stockageService.urlPublique("bucket-photo-immobilier", "a.jpg"));
        assertNull(stockageService.urlPublique("bucket-photo-immobilier", null));
        assertNull(stockageService.urlPublique("bucket-photo-immobilier", ""));
    }

    @Test
    @DisplayName("S2 : photo de profil signée pour la durée demandée (24 h)")
    void urlPresignee_dureeChoisie() throws Exception {
        PresignedGetObjectRequest signee = mock(PresignedGetObjectRequest.class);
        when(signee.url()).thenReturn(URI.create("http://localhost:9000/bucket-photo-profil/a.jpg?X-Amz-Signature=s").toURL());
        when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class))).thenReturn(signee);

        stockageService.urlPresignee("bucket-photo-profil", "a.jpg", Duration.ofHours(24));

        ArgumentCaptor<GetObjectPresignRequest> captor = ArgumentCaptor.forClass(GetObjectPresignRequest.class);
        verify(s3Presigner).presignGetObject(captor.capture());
        assertEquals(Duration.ofHours(24), captor.getValue().signatureDuration());
    }

    // =======================
    // Suppression, URL publique
    // =======================

    @Test
    @DisplayName("supprimer : une erreur du stockage est journalisée, pas propagée")
    void supprimer_erreurIgnoree() {
        doThrow(S3Exception.builder().statusCode(500).build()).when(s3Client).deleteObject(any(DeleteObjectRequest.class));

        assertDoesNotThrow(() -> stockageService.supprimer("bucket-recus-paiement", "cle.jpg"));
    }

    @Test
    @DisplayName("URL publique en virtual-host (path-style désactivé) : <bucket>.<hôte>/<clé>")
    void urlPublique_virtualHost() {
        StockageProperties virtuel = new StockageProperties(
                "http://stockage:9000", "https://fichiers.kupanga.test", "auto", false, "c", "s", false);

        assertEquals("https://bucket-photo-immobilier.fichiers.kupanga.test/a.jpg",
                virtuel.urlObjet("bucket-photo-immobilier", "a.jpg"));
        assertEquals("http://localhost:9000/bucket-photo-immobilier/a.jpg",
                PROPRIETES.urlObjet("bucket-photo-immobilier", "a.jpg"), "Slash final de l'URL publique retiré");
    }
}
