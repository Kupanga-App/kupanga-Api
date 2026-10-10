package com.kupanga.api.config;

import jakarta.servlet.MultipartConfigElement;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * B3 : limites d'upload réellement appliquées par le conteneur de servlets
 * (MockMvc ne les applique pas, d'où le contrôle de la configuration multipart).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@DisplayName("Tests — limites d'upload (B3)")
class MultipartConfigTest {

    private static final long MO = 1024L * 1024L;

    @Autowired private MultipartConfigElement multipartConfig;

    @Test
    @DisplayName("10 Mo par fichier, 50 Mo par requête (plusieurs photos de smartphone)")
    void limitesUpload() {
        assertThat(multipartConfig.getMaxFileSize()).isEqualTo(10 * MO);
        assertThat(multipartConfig.getMaxRequestSize()).isEqualTo(50 * MO);
    }
}
