package com.kupanga.api.config;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * D2/D7 : garde-fous sur le {@code Dockerfile} (lu à la racine du projet, répertoire de travail de Maven).
 */
@DisplayName("Dockerfile (D2, D7)")
class DockerfileTest {

    private static String dockerfile;

    @BeforeAll
    static void lire() throws IOException {
        // Fins de ligne normalisées (dépôt cloné sous Windows)
        dockerfile = Files.readString(Path.of("Dockerfile"), StandardCharsets.UTF_8).replace("\r\n", "\n");
    }

    @Test
    @DisplayName("D2 : l'application tourne avec un utilisateur non root, le JAR reste à root")
    void nonRoot() {
        assertThat(dockerfile).contains("\nUSER app\n");
        assertThat(dockerfile).doesNotContain("--chown");
    }

    @Test
    @DisplayName("D2 : profil prod par défaut (une variable oubliée ne démarre pas en dev)")
    void profilProdParDefaut() {
        assertThat(dockerfile).contains("ENV SPRING_PROFILES_ACTIVE=prod");
    }

    @Test
    @DisplayName("D2 : mémoire JVM bornée par le conteneur, sonde de vivacité, java en PID 1 (SIGTERM)")
    void jvmEtHealthcheck() {
        assertThat(dockerfile).contains("-XX:MaxRAMPercentage=75");
        assertThat(dockerfile).contains("HEALTHCHECK").contains("/actuator/health/liveness");
        assertThat(dockerfile).contains("exec java $JAVA_OPTS");
    }

    @Test
    @DisplayName("D7 : images de base figées (pas de latest ni d'étiquette majeure seule)")
    void versionsFigees() {
        assertThat(dockerfile).doesNotContain(":latest");
        assertThat(dockerfile).containsPattern("FROM eclipse-temurin:\\d+\\.\\d+\\.\\d+_\\d+-jdk-alpine AS build");
        assertThat(dockerfile).containsPattern("FROM eclipse-temurin:\\d+\\.\\d+\\.\\d+_\\d+-jre-alpine\\n");
    }
}
