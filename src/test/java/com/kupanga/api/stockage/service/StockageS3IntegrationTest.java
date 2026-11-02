package com.kupanga.api.stockage.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.kupanga.api.stockage.config.StockageConfig;
import com.kupanga.api.stockage.config.StockageProperties;
import com.kupanga.api.stockage.image.ImagesDeTest;
import com.kupanga.api.stockage.image.PhotoPreparee;
import com.kupanga.api.stockage.service.impl.PreparationBuckets;
import com.kupanga.api.stockage.service.impl.StockageServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockMultipartFile;
import software.amazon.awssdk.services.s3.S3Client;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static com.kupanga.api.stockage.constant.StockageConstant.CONTRAT_BUCKET;
import static com.kupanga.api.stockage.constant.StockageConstant.DUREE_URL_PHOTO_PROFIL;
import static com.kupanga.api.stockage.constant.StockageConstant.PHOTO_IMO_BUCKET;
import static com.kupanga.api.stockage.constant.StockageConstant.PHOTO_PROFIL_BUCKET;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * S1 : parcours réel sur un serveur S3 (CI et dev : localhost:9000, identifiants de développement). Ignoré si aucun
 * serveur S3 n'écoute : le reste de la suite n'en a pas besoin (stockage simulé).
 */
@DisplayName("Tests d'intégration — stockage sur un vrai serveur S3")
class StockageS3IntegrationTest {

    /** Serveur S3 de test : -Dstockage.test.url=... (défaut : localhost:9000, comme en CI). */
    private static final String URL = System.getProperty("stockage.test.url", "http://localhost:9000");

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
    private StockageServiceImpl stockage;
    private ListAppender<ILoggingEvent> logs;

    @BeforeEach
    void setUp() {
        assumeTrue(serveurJoignable(), "Aucun serveur S3 sur " + URL + " : test ignoré");
        StockageProperties proprietes = new StockageProperties(
                URL, URL, "us-east-1", true, "kupanga-dev", "kupanga-dev-secret", true);
        StockageConfig config = new StockageConfig();
        S3Client client = config.s3Client(proprietes);
        PreparationBuckets preparation = new PreparationBuckets(client, config.s3ClientAnonyme(proprietes), proprietes);
        logs = new ListAppender<>();
        logs.start();
        ((Logger) LoggerFactory.getLogger(PreparationBuckets.class)).addAppender(logs);
        preparation.assurer();
        stockage = new StockageServiceImpl(client, config.s3Presigner(proprietes), proprietes, preparation);
    }

    @AfterEach
    void tearDown() {
        if (logs != null) {
            ((Logger) LoggerFactory.getLogger(PreparationBuckets.class)).detachAppender(logs);
        }
    }

    private boolean serveurJoignable() {
        try {
            http.send(HttpRequest.newBuilder(URI.create(URL)).timeout(Duration.ofSeconds(3)).GET().build(),
                    HttpResponse.BodyHandlers.discarding());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private HttpResponse<byte[]> lireSansIdentifiants(String url) throws Exception {
        return http.send(HttpRequest.newBuilder(URI.create(url)).GET().build(), HttpResponse.BodyHandlers.ofByteArray());
    }

    @Test
    @DisplayName("P0-7 : PDF privé illisible sans signature, lisible par URL présignée ; aucune alerte de confidentialité")
    void pdfPrive() throws Exception {
        byte[] pdf = "%PDF-1.4 test".getBytes();
        String cle = stockage.uploadPdf(pdf, "contrat.pdf", CONTRAT_BUCKET);

        assertThat(lireSansIdentifiants(URL + "/" + CONTRAT_BUCKET + "/" + cle).statusCode()).isEqualTo(403);
        HttpResponse<byte[]> signee = lireSansIdentifiants(stockage.urlPresignee(CONTRAT_BUCKET, cle));
        assertThat(signee.statusCode()).isEqualTo(200);
        assertThat(signee.body()).isEqualTo(pdf);
        assertThat(stockage.telecharger(CONTRAT_BUCKET, cle)).isEqualTo(pdf);
        assertThat(logs.list).noneMatch(e -> e.getLevel() == Level.ERROR);

        stockage.supprimer(CONTRAT_BUCKET, cle);
    }

    @Test
    @DisplayName("Photo publique lisible sans identifiants, avec le type MIME fixé par le serveur")
    void photoPublique() throws Exception {
        String cle = stockage.envoyerImage(PhotoPreparee.pourBucketPublic(
                new MockMultipartFile("f", "a.png", "image/png", ImagesDeTest.png(60, 40)), 400), PHOTO_IMO_BUCKET);
        String url = stockage.urlPublique(PHOTO_IMO_BUCKET, cle);

        HttpResponse<byte[]> reponse = lireSansIdentifiants(url);
        assertThat(reponse.statusCode()).isEqualTo(200);
        assertThat(reponse.headers().firstValue("Content-Type")).contains("image/jpeg");

        stockage.supprimer(PHOTO_IMO_BUCKET, cle);
        assertThat(lireSansIdentifiants(url).statusCode()).isIn(403, 404);
    }

    @Test
    @DisplayName("S2 : photo de profil privée, lisible seulement par URL présignée")
    void photoProfilPrivee() throws Exception {
        String cle = stockage.envoyerImage(PhotoPreparee.pourBucketPublic(
                new MockMultipartFile("f", "a.png", "image/png", ImagesDeTest.png(60, 40)), 400), PHOTO_PROFIL_BUCKET);

        assertThat(lireSansIdentifiants(URL + "/" + PHOTO_PROFIL_BUCKET + "/" + cle).statusCode()).isEqualTo(403);
        assertThat(lireSansIdentifiants(stockage.urlPresignee(PHOTO_PROFIL_BUCKET, cle, DUREE_URL_PHOTO_PROFIL))
                .statusCode()).isEqualTo(200);

        stockage.supprimer(PHOTO_PROFIL_BUCKET, cle);
    }
}
