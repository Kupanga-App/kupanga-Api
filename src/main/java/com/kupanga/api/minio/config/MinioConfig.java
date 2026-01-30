package com.kupanga.api.minio.config;

import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MinioConfig {

    @Value("${minio.access-key}")
    private String accessKey ;

    @Value("${minio.secret-key}")
    private String secretKey ;

    @Value("${app.url-mino}")
    private String url_minio;

    /**
     * Création du client MinIO avec endpoint et les credentials
     * @return client MinIO
     */
    @Bean
    public MinioClient minioClient(){

        return MinioClient.builder()
                .endpoint(url_minio)
                .credentials(accessKey , secretKey)
                .build();
    }
}
