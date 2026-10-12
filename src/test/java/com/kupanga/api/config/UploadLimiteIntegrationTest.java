package com.kupanga.api.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * B3 : dépassement des limites d'upload sur un vrai serveur (MockMvc n'analyse pas le multipart comme Tomcat).
 * Route publique {@code POST /auth/register} : la limite REGISTER_PAR_IP (5 / h) passe avant la lecture du corps.
 * Limite ramenée à 1 Ko ici : avec un vrai fichier de 10 Mo, Tomcat ferme la connexion après l'erreur pendant
 * que le client écrit encore (le front doit contrôler la taille avant l'envoi). Les valeurs réelles
 * (10 Mo / 50 Mo) sont vérifiées par {@link MultipartConfigTest}.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.servlet.multipart.max-file-size=1KB"
})
@DisplayName("Tests d'intégration — limites d'upload (B3)")
class UploadLimiteIntegrationTest {

    private static final int TROP_GROS = 2 * 1024;

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private ObjectMapper objectMapper;

    @Test
    @DisplayName("Fichier trop gros → 413 au format de l'API ; limite d'inscription atteinte → 429 sans lire le fichier")
    void fichierTropGros_413_puisLimiteAvantLecture_429() throws Exception {
        // 1re tentative : fichier au-delà de la limite → 413 renvoyé par l'API (ApiErrorResponse), pas une 500 ni la page /error
        ResponseEntity<String> tropGros = inscrire(TROP_GROS);
        assertThat(tropGros.getStatusCode().value()).isEqualTo(413);
        JsonNode corps = objectMapper.readTree(tropGros.getBody());
        assertThat(corps.get("status").asInt()).isEqualTo(413);
        assertThat(corps.get("path").asText()).isEqualTo("/auth/register");

        // Tentatives 2 à 5 (petites, refusées pour d'autres raisons) : le seau REGISTER_PAR_IP se vide
        for (int i = 0; i < 4; i++) {
            assertThat(inscrire(1).getStatusCode().value()).isNotEqualTo(429);
        }

        // 6e : 429 et non 413 → la limite est vérifiée avant l'analyse du corps
        ResponseEntity<String> bloque = inscrire(TROP_GROS);
        assertThat(bloque.getStatusCode().value()).isEqualTo(429);
        assertThat(bloque.getHeaders().getFirst(HttpHeaders.RETRY_AFTER)).isNotBlank();
        assertThat(objectMapper.readTree(bloque.getBody()).get("status").asInt()).isEqualTo(429);
    }

    private ResponseEntity<String> inscrire(int tailleImage) {
        HttpHeaders jsonHeaders = new HttpHeaders();
        jsonHeaders.setContentType(MediaType.APPLICATION_JSON);
        HttpHeaders imageHeaders = new HttpHeaders();
        imageHeaders.setContentType(MediaType.IMAGE_JPEG);

        MultiValueMap<String, Object> parties = new LinkedMultiValueMap<>();
        parties.add("userFormDTO", new HttpEntity<>("{}", jsonHeaders));
        parties.add("imageProfil", new HttpEntity<>(new ByteArrayResource(new byte[tailleImage]) {
            @Override
            public String getFilename() {
                return "photo.jpg";
            }
        }, imageHeaders));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        return restTemplate.postForEntity("/auth/register", new HttpEntity<>(parties, headers), String.class);
    }
}
