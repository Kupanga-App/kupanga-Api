package com.kupanga.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Origines autorisées à appeler l'API (CORS) et à ouvrir le WebSocket (A11/W5).
 * Propriété {@code app.cors.allowed-origins} (variable d'environnement {@code CORS_ALLOWED_ORIGINS},
 * valeurs séparées par des virgules). Le joker {@code *} est refusé : avec les cookies
 * (refresh token), il permettrait à n'importe quel site de récupérer un access token.
 *
 * @param allowedOrigins origines exactes, ex. {@code https://kupanga.lespacelibellule.com}
 */
@ConfigurationProperties(prefix = "app.cors")
public record CorsProperties(List<String> allowedOrigins) {

    private static final Pattern FORMAT_ORIGINE = Pattern.compile("^https?://[^/\\s]+$");

    public CorsProperties {
        if (allowedOrigins == null || allowedOrigins.isEmpty()) {
            throw new IllegalStateException("app.cors.allowed-origins doit lister au moins une origine");
        }
        allowedOrigins = allowedOrigins.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(origine -> !origine.isEmpty())
                // Une origine n'a jamais de « / » final (sinon le navigateur ne la reconnaît pas)
                .map(origine -> origine.endsWith("/") ? origine.substring(0, origine.length() - 1) : origine)
                .toList();
        if (allowedOrigins.isEmpty()) {
            throw new IllegalStateException("app.cors.allowed-origins doit lister au moins une origine");
        }
        if (allowedOrigins.stream().anyMatch(origine -> origine.contains("*"))) {
            throw new IllegalStateException("app.cors.allowed-origins ne doit pas contenir de joker « * »");
        }
        allowedOrigins.stream()
                .filter(origine -> !FORMAT_ORIGINE.matcher(origine).matches())
                .findFirst()
                .ifPresent(origine -> {
                    // Sinon l'origine ne correspondrait jamais à rien et le front serait bloqué sans message
                    throw new IllegalStateException("Origine CORS invalide (attendu : schéma://hôte[:port], sans chemin) : " + origine);
                });
    }
}
