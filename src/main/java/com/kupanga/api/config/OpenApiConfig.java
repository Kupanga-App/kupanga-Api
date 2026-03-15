package com.kupanga.api.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Value("${app.url:http://localhost:8089}")
    private String serverUrl;

    @Bean
    public OpenAPI kupangaOpenAPI() {
        return new OpenAPI()

                // ─── Informations générales ───────────────────────────────────────────
                .info(new Info()
                        .title("Kupanga API")
                        .description("""
                                Documentation interactive de l'API Kupanga — plateforme de gestion immobilière.
                                
                                **Fonctionnalités disponibles :**
                                - Authentification (register, login, refresh, logout)
                                - Gestion des biens immobiliers
                                - Upload de fichiers via MinIO
                                - Gestion des contrats, quittances et documents
                                
                                **Authentification :** Cliquez sur le bouton 🔒 **Authorize** en haut à droite,
                                puis saisissez votre token JWT au format `Bearer <token>`.
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Support Kupanga")
                                .email("noreplydevback@gmail.com")
                                .url("https://kupanga.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")))

                // ─── Serveurs ─────────────────────────────────────────────────────────
                .servers(List.of(
                        new Server()
                                .url(serverUrl)
                                .description("Serveur local pour le front Angular"),
                        new Server()
                                .url("http://localhost:8089")
                                .description("Développement local Spring Boot"),
                        new Server()
                                .url("http://localhost:8080")
                                .description("Docker local")
                ))

                // ─── Sécurité JWT globale ─────────────────────────────────────────────
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth",
                                new SecurityScheme()
                                        .name("bearerAuth")
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Saisissez votre token JWT. " +
                                                "Obtenez-le via POST /auth/login")));
    }
}