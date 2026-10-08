package com.kupanga.api.minio.config;

import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class MinioConfig {

    @Value("${minio.access-key}")
    private String accessKey ;

    @Value("${minio.secret-key}")
    private String secretKey ;

    @Value("${minio.endpoint}")
    private String url_minio;

    /**
     * Création du client MinIO avec endpoint et les credentials
     * @return client MinIO
     */
    @Bean
    @Primary
    public MinioClient minioClient(){

        return MinioClient.builder()
                .endpoint(url_minio)
                .credentials(accessKey , secretKey)
                .build();
    }

    /**
     * Client utilisé uniquement pour signer les URL présignées (P0-7).
     * La signature dépend de l'hôte : elle doit être calculée avec l'URL publique de MinIO,
     * celle que le navigateur appellera. La région (propriété {@code minio.region}, défaut us-east-1, doit être celle du serveur MinIO)
     * est fixée pour éviter tout appel réseau au moment de signer.
     * @param urlPublique URL publique de MinIO
     * @param region région du serveur MinIO
     * @return client MinIO pointant vers l'URL publique
     */
    @Bean(name = "minioPresignClient")
    public MinioClient minioPresignClient(@Value("${app.url-mino}") String urlPublique,
                                          @Value("${minio.region:us-east-1}") String region){

        return MinioClient.builder()
                .endpoint(urlPublique)
                .credentials(accessKey , secretKey)
                .region(region)
                .build();
    }
}
