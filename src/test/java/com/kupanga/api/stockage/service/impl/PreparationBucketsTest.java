package com.kupanga.api.stockage.service.impl;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.kupanga.api.stockage.config.StockageProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteBucketPolicyRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.PutBucketPolicyRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;

import static com.kupanga.api.stockage.constant.StockageConstant.BUCKETS_PRIVES;
import static com.kupanga.api.stockage.constant.StockageConstant.BUCKETS_PUBLICS;
import static com.kupanga.api.stockage.constant.StockageConstant.CONTRAT_BUCKET;
import static com.kupanga.api.stockage.constant.StockageConstant.PHOTO_IMO_BUCKET;
import static com.kupanga.api.stockage.constant.StockageConstant.RECU_BUCKET;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("S1 : préparation des buckets au démarrage")
class PreparationBucketsTest {

    private S3Client s3Client;
    private S3Client anonyme;
    private ListAppender<ILoggingEvent> logs;

    @BeforeEach
    void setUp() {
        s3Client = mock(S3Client.class);
        anonyme = mock(S3Client.class);
        // Par défaut, la lecture anonyme est refusée (bucket privé conforme)
        when(anonyme.getObjectAsBytes(any(GetObjectRequest.class)))
                .thenThrow(S3Exception.builder().statusCode(403).build());
        logs = new ListAppender<>();
        logs.start();
        ((Logger) LoggerFactory.getLogger(PreparationBuckets.class)).addAppender(logs);
    }

    @AfterEach
    void tearDown() {
        ((Logger) LoggerFactory.getLogger(PreparationBuckets.class)).detachAppender(logs);
    }

    private PreparationBuckets preparation(boolean gererPolitiques) {
        return new PreparationBuckets(s3Client, anonyme, new StockageProperties(
                "http://s3:9000", "http://localhost:9000", "us-east-1", true, "c", "s", gererPolitiques));
    }

    @Test
    @DisplayName("Buckets absents créés ; lecture publique posée sur les buckets publics, retirée des privés ; une seule fois")
    void creeEtPosePolitiques() {
        when(s3Client.headBucket(any(HeadBucketRequest.class))).thenThrow(NoSuchBucketException.builder().build());
        PreparationBuckets preparation = preparation(true);

        preparation.assurer();
        preparation.assurer();

        int total = BUCKETS_PUBLICS.size() + BUCKETS_PRIVES.size();
        verify(s3Client, times(total)).createBucket(any(CreateBucketRequest.class));
        verify(s3Client, times(BUCKETS_PUBLICS.size())).putBucketPolicy(any(PutBucketPolicyRequest.class));
        verify(s3Client, times(BUCKETS_PRIVES.size())).deleteBucketPolicy(any(DeleteBucketPolicyRequest.class));
        verify(s3Client, times(BUCKETS_PRIVES.size())).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    @DisplayName("gerer-politiques=false (ex. Garage) : aucune politique touchée, buckets existants non recréés")
    void sansPolitiques() {
        preparation(false).assurer();

        verify(s3Client, never()).createBucket(any(CreateBucketRequest.class));
        verify(s3Client, never()).putBucketPolicy(any(PutBucketPolicyRequest.class));
        verify(s3Client, never()).deleteBucketPolicy(any(DeleteBucketPolicyRequest.class));
    }

    @Test
    @DisplayName("Serveur sans politiques de bucket (501) : journalisé, la préparation continue")
    void politiqueRefuseeParLeServeur() {
        doThrow(S3Exception.builder().statusCode(501).build()).when(s3Client).putBucketPolicy(any(PutBucketPolicyRequest.class));

        preparation(true).assurer();

        verify(s3Client, times(BUCKETS_PRIVES.size())).deleteBucketPolicy(any(DeleteBucketPolicyRequest.class));
        assertThat(logs.list).anyMatch(e -> e.getLevel() == Level.WARN && e.getFormattedMessage().contains("non appliquée"));
    }

    @Test
    @DisplayName("P0-7 : bucket privé lisible sans authentification → log ERROR")
    void bucketPriveLisible_erreur() {
        reset(anonyme);
        when(anonyme.getObjectAsBytes(any(GetObjectRequest.class)))
                .thenReturn(ResponseBytes.fromByteArray(GetObjectResponse.builder().build(), "x".getBytes()));

        PreparationBuckets preparation = preparation(true);
        preparation.assurer();

        assertThat(logs.list).filteredOn(e -> e.getLevel() == Level.ERROR)
                .hasSize(BUCKETS_PRIVES.size())
                .allMatch(e -> e.getFormattedMessage().contains("LISIBLE SANS AUTHENTIFICATION"));
        // Échec fermé (revue S1) : plus aucun document envoyé dans un bucket privé exposé
        assertThatThrownBy(() -> preparation.assurerEcriture(CONTRAT_BUCKET)).isInstanceOf(IllegalStateException.class);
        preparation.assurerEcriture(PHOTO_IMO_BUCKET);
    }

    @Test
    @DisplayName("P0-7 : retrait de la politique refusé (403, compte de service limité) et bucket resté public → envois refusés")
    void retraitPolitiqueRefuse_bucketExpose() {
        doThrow(S3Exception.builder().statusCode(403).build()).when(s3Client).deleteBucketPolicy(any(DeleteBucketPolicyRequest.class));
        reset(anonyme);
        when(anonyme.getObjectAsBytes(any(GetObjectRequest.class)))
                .thenReturn(ResponseBytes.fromByteArray(GetObjectResponse.builder().build(), "x".getBytes()));
        PreparationBuckets preparation = preparation(true);

        assertThatThrownBy(() -> preparation.assurerEcriture(RECU_BUCKET)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("Compte de service sans s3:ListBucket (HeadBucket en 403) : buckets supposés existants, envois possibles")
    void headBucketRefuse_envoisPossibles() {
        when(s3Client.headBucket(any(HeadBucketRequest.class))).thenThrow(S3Exception.builder().statusCode(403).build());
        PreparationBuckets preparation = preparation(true);

        preparation.assurerEcriture(CONTRAT_BUCKET);

        verify(s3Client, never()).createBucket(any(CreateBucketRequest.class));
    }

    @Test
    @DisplayName("Bucket privé conforme (403 en anonyme) : aucune erreur")
    void bucketPriveConforme() {
        preparation(true).assurer();

        assertThat(logs.list).noneMatch(e -> e.getLevel() == Level.ERROR);
    }

    @Test
    @DisplayName("Stockage injoignable au démarrage : l'application démarre ; nouvel essai au plus toutes les 30 s, échec immédiat entre-temps")
    void injoignableAuDemarrage() {
        when(s3Client.headBucket(any(HeadBucketRequest.class))).thenThrow(SdkClientException.create("connexion refusée"));
        AtomicReference<Instant> maintenant = new AtomicReference<>(Instant.parse("2026-11-02T10:00:00Z"));
        Clock horloge = new Clock() {
            @Override public ZoneId getZone() { return ZoneOffset.UTC; }
            @Override public Clock withZone(ZoneId zone) { return this; }
            @Override public Instant instant() { return maintenant.get(); }
        };
        PreparationBuckets preparation = new PreparationBuckets(s3Client, anonyme, new StockageProperties(
                "http://s3:9000", "http://localhost:9000", "us-east-1", true, "c", "s", true), horloge);

        preparation.auDemarrage();
        verify(s3Client, times(1)).headBucket(any(HeadBucketRequest.class));

        // Dans les 30 s : échec immédiat, sans appeler le stockage
        assertThatThrownBy(preparation::assurer).isInstanceOf(IllegalStateException.class);
        verify(s3Client, times(1)).headBucket(any(HeadBucketRequest.class));

        // Après 30 s : nouvel essai
        maintenant.set(maintenant.get().plusSeconds(31));
        reset(s3Client);
        preparation.assurerEcriture(CONTRAT_BUCKET);
        verify(s3Client, atLeastOnce()).headBucket(any(HeadBucketRequest.class));
    }
}
