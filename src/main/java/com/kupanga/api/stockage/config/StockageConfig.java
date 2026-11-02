package com.kupanga.api.stockage.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.core.checksums.ResponseChecksumValidation;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;
import java.time.Duration;

/**
 * S1 : clients S3 du SDK AWS v2, configurés uniquement par {@link StockageProperties}.
 * <p>
 * Sommes de contrôle CRC envoyées seulement quand l'API S3 les exige : plusieurs serveurs compatibles S3
 * refusent les en-têtes ajoutés par défaut depuis le SDK 2.30.
 */
@Configuration
@EnableConfigurationProperties(StockageProperties.class)
public class StockageConfig {

    /** Client principal : envois, lectures et administration des buckets, sur l'URL interne. */
    @Bean
    @Primary
    public S3Client s3Client(StockageProperties proprietes) {
        return client(proprietes, identifiants(proprietes));
    }

    /**
     * Client <b>sans identifiants</b>, utilisé seulement au démarrage pour vérifier qu'un bucket privé n'est pas
     * lisible par n'importe qui (P0-7).
     */
    @Bean(name = "s3ClientAnonyme")
    public S3Client s3ClientAnonyme(StockageProperties proprietes) {
        return client(proprietes, AnonymousCredentialsProvider.create());
    }

    /**
     * Signe les URL présignées (P0-7). La signature dépend de l'hôte : elle est calculée avec l'URL publique,
     * celle que le navigateur appellera. La région est fixée : aucun appel réseau au moment de signer.
     */
    @Bean
    public S3Presigner s3Presigner(StockageProperties proprietes) {
        return S3Presigner.builder()
                .endpointOverride(URI.create(proprietes.urlPublique()))
                .region(Region.of(proprietes.region()))
                .credentialsProvider(identifiants(proprietes))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(proprietes.pathStyle())
                        .build())
                .build();
    }

    private static S3Client client(StockageProperties proprietes, AwsCredentialsProvider identifiants) {
        return S3Client.builder()
                .endpointOverride(URI.create(proprietes.endpoint()))
                .region(Region.of(proprietes.region()))
                .credentialsProvider(identifiants)
                .forcePathStyle(proprietes.pathStyle())
                .requestChecksumCalculation(RequestChecksumCalculation.WHEN_REQUIRED)
                .responseChecksumValidation(ResponseChecksumValidation.WHEN_REQUIRED)
                .httpClient(UrlConnectionHttpClient.builder()
                        .connectionTimeout(Duration.ofSeconds(10))
                        .socketTimeout(Duration.ofSeconds(30))
                        .build())
                .build();
    }

    private static AwsCredentialsProvider identifiants(StockageProperties proprietes) {
        return StaticCredentialsProvider.create(
                AwsBasicCredentials.create(proprietes.accessKey(), proprietes.secretKey()));
    }
}
