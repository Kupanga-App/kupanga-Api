package com.kupanga.api.stockage.service.impl;

import com.kupanga.api.stockage.config.StockageProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteBucketPolicyRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.PutBucketPolicyRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.kupanga.api.stockage.constant.StockageConstant.BUCKETS_PRIVES;
import static com.kupanga.api.stockage.constant.StockageConstant.BUCKETS_PUBLICS;

/**
 * S1 : prépare les buckets <b>une fois</b>, au démarrage, et plus jamais à chaque envoi (plusieurs serveurs S3 ne
 * gèrent pas les politiques de bucket : poser une politique à l'envoi faisait échouer tous les envois).
 * <ul>
 *     <li>crée les buckets absents ;</li>
 *     <li>si {@code stockage.gerer-politiques} : lecture publique sur les buckets publics, aucune politique
 *     sur les buckets privés ; une politique refusée par le serveur est journalisée, sans bloquer ;</li>
 *     <li>vérifie qu'aucun bucket privé n'est lisible sans authentification (P0-7) : sinon log {@code ERROR} et
 *     <b>plus aucun envoi</b> dans ce bucket ({@link #assurerEcriture}) tant que l'application n'a pas redémarré
 *     avec un bucket redevenu privé.</li>
 * </ul>
 * Stockage injoignable au démarrage : l'application démarre quand même, la préparation est retentée au premier envoi,
 * au plus une fois toutes les {@value #DELAI_NOUVEL_ESSAI_SECONDES} secondes (sinon échec immédiat).
 */
@Slf4j
@Component
public class PreparationBuckets {

    /** Objet témoin écrit dans chaque bucket privé puis relu sans identifiants. */
    static final String CLE_SONDE = "sonde-confidentialite.txt";

    static final long DELAI_NOUVEL_ESSAI_SECONDES = 30;

    private final S3Client s3Client;
    private final S3Client s3ClientAnonyme;
    private final StockageProperties proprietes;
    private final Clock horloge;

    private volatile boolean prets;
    private volatile Instant dernierEchec;
    /** Buckets privés trouvés lisibles sans authentification : aucun envoi n'y est fait (échec fermé, P0-7). */
    private final Set<String> bucketsExposes = ConcurrentHashMap.newKeySet();

    @Autowired
    public PreparationBuckets(S3Client s3Client,
                              @Qualifier("s3ClientAnonyme") S3Client s3ClientAnonyme,
                              StockageProperties proprietes) {
        this(s3Client, s3ClientAnonyme, proprietes, Clock.systemUTC());
    }

    PreparationBuckets(S3Client s3Client, S3Client s3ClientAnonyme, StockageProperties proprietes, Clock horloge) {
        this.s3Client = s3Client;
        this.s3ClientAnonyme = s3ClientAnonyme;
        this.proprietes = proprietes;
        this.horloge = horloge;
    }

    /**
     * Prépare les buckets si besoin, puis refuse l'envoi dans un bucket privé exposé publiquement.
     * @param bucket bucket dans lequel on va écrire
     */
    public void assurerEcriture(String bucket) {
        assurer();
        if (bucketsExposes.contains(bucket)) {
            throw new IllegalStateException("Envoi refusé : le bucket privé " + bucket + " est lisible publiquement");
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    public void auDemarrage() {
        try {
            assurer();
        } catch (Exception e) {
            log.warn("Stockage S3 injoignable au démarrage, préparation des buckets reportée : {}",
                    e.getClass().getSimpleName());
        }
    }

    /** Prépare les buckets si ce n'est pas déjà fait ; lève une exception si le stockage est injoignable. */
    public void assurer() {
        if (prets) {
            return;
        }
        synchronized (this) {
            if (prets) {
                return;
            }
            // Stockage injoignable : pas une nouvelle tentative (jusqu'à ~30 s) par envoi, sous ce verrou
            Instant echec = dernierEchec;
            if (echec != null && horloge.instant().isBefore(echec.plusSeconds(DELAI_NOUVEL_ESSAI_SECONDES))) {
                throw new IllegalStateException("Stockage indisponible");
            }
            try {
                BUCKETS_PUBLICS.forEach(bucket -> preparer(bucket, true));
                BUCKETS_PRIVES.forEach(bucket -> preparer(bucket, false));
                BUCKETS_PRIVES.forEach(this::verifierPrive);
            } catch (RuntimeException e) {
                dernierEchec = horloge.instant();
                throw e;
            }
            prets = true;
        }
    }

    private void preparer(String bucket, boolean lecturePublique) {
        creerSiAbsent(bucket);
        if (!proprietes.gererPolitiques()) {
            return;
        }
        try {
            if (lecturePublique) {
                s3Client.putBucketPolicy(PutBucketPolicyRequest.builder()
                        .bucket(bucket).policy(politiqueLecturePublique(bucket)).build());
            } else {
                s3Client.deleteBucketPolicy(DeleteBucketPolicyRequest.builder().bucket(bucket).build());
            }
        } catch (S3Exception e) {
            log.warn("Politique du bucket {} non appliquée par le serveur (HTTP {}) : droits à régler au déploiement",
                    bucket, e.statusCode());
        }
    }

    private void creerSiAbsent(String bucket) {
        try {
            s3Client.headBucket(HeadBucketRequest.builder().bucket(bucket).build());
        } catch (NoSuchBucketException e) {
            s3Client.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
            log.info("Bucket {} créé", bucket);
        } catch (S3Exception e) {
            if (e.statusCode() != 403) {
                throw e;
            }
            // Revue de fin de Sprint 5 : compte de service sans s3:ListBucket → on suppose le bucket existant
            // (les droits sur les objets suffisent à l'application) plutôt que de bloquer tous les envois
            log.warn("Existence du bucket {} non vérifiable (HTTP 403, droit s3:ListBucket absent) : supposé existant",
                    bucket);
        }
    }

    /** P0-7 : écrit un objet témoin puis tente de le lire sans identifiants ; une lecture réussie est une fuite. */
    void verifierPrive(String bucket) {
        s3Client.putObject(PutObjectRequest.builder().bucket(bucket).key(CLE_SONDE).contentType("text/plain").build(),
                RequestBody.fromString("Kupanga : ce fichier ne doit pas être lisible publiquement",
                        StandardCharsets.UTF_8));
        try {
            s3ClientAnonyme.getObjectAsBytes(GetObjectRequest.builder().bucket(bucket).key(CLE_SONDE).build());
            bucketsExposes.add(bucket);
            log.error("Le bucket privé {} est LISIBLE SANS AUTHENTIFICATION : envois refusés ; retirer sa politique "
                    + "publique puis redémarrer (P0-7)", bucket);
        } catch (S3Exception e) {
            if (e.statusCode() != 403) {
                log.warn("Confidentialité du bucket {} non vérifiée (HTTP {})", bucket, e.statusCode());
            }
        } catch (Exception e) {
            log.warn("Confidentialité du bucket {} non vérifiée : {}", bucket, e.getClass().getSimpleName());
        }
    }

    static String politiqueLecturePublique(String bucket) {
        return """
                {
                  "Version": "2012-10-17",
                  "Statement": [
                    {
                      "Effect": "Allow",
                      "Principal": "*",
                      "Action": ["s3:GetObject"],
                      "Resource": ["arn:aws:s3:::%s/*"]
                    }
                  ]
                }""".formatted(bucket);
    }
}
