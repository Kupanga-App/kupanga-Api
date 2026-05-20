package com.kupanga.api.email.client;

import com.kupanga.api.email.dto.BrevoEmail;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Component
@Slf4j
public class BrevoEmailClient {

    private final WebClient webClient;

    public BrevoEmailClient(@Value("${brevo.api-key}") String apiKey) {
        this.webClient = WebClient.builder()
                .baseUrl("https://api.brevo.com/v3")
                .defaultHeader("api-key", apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public void send(BrevoEmail email) {
        try {
            webClient.post()
                    .uri("/smtp/email")
                    .bodyValue(email)
                    .retrieve()
                    .bodyToMono(Void.class)
                    .block();
            log.info("Brevo API — email envoyé à {}", email.to().get(0).email());
        } catch (WebClientResponseException e) {
            log.error("Brevo API erreur HTTP {} pour {} : {}",
                    e.getStatusCode(), email.to().get(0).email(), e.getResponseBodyAsString());
            throw new RuntimeException("Erreur lors de l'envoi via Brevo API", e);
        } catch (Exception e) {
            log.error("Brevo API erreur inattendue pour {} : {}", email.to().get(0).email(), e.getMessage(), e);
            throw new RuntimeException("Erreur lors de l'envoi via Brevo API", e);
        }
    }
}
