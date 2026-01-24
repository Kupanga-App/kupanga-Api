package com.kupanga.api.config;

import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MinioConfig {

    @Value("${minio.endpoint}")
    private String url ;

    @Value("${minio.access-key}")
    private String accessKey ;

    @Value("${minio.secret-key}")
    private String secretKey ;

    /**
     * Création du client MinIO avec endpoint et les credentials
     * @return client MinIO
     */
    @Bean
    public MinioClient minioClient(){

        return MinioClient.builder()
                .endpoint(url)
                .credentials(accessKey , secretKey)
                .build();
    }
}
