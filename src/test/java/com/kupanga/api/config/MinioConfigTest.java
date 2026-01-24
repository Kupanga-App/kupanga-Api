package com.kupanga.api.config;

import io.minio.MinioClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(
        classes = MinioConfig.class,
        properties = {
                "minio.endpoint=http://localhost:9000",
                "minio.access-key=minioadmin",
                "minio.secret-key=minioadmin"
        }
)
@DisplayName("MinioConfig test")
class MinioConfigTest {

    @Autowired
    private MinioClient minioClient;

    @Test
    void shouldCreateMinioClientBean() {
        assertNotNull(minioClient);
    }
}

