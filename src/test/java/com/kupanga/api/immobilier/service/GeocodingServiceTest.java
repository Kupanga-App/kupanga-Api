package com.kupanga.api.immobilier.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Point;
import org.mockito.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("Tests unitaires — GeocodingService")
@SuppressWarnings("unchecked")
class GeocodingServiceTest {

    @Mock private WebClient                         webClient;
    @Mock private WebClient.RequestHeadersUriSpec   requestHeadersUriSpec;
    @Mock private WebClient.RequestHeadersSpec      requestHeadersSpec;
    @Mock private WebClient.ResponseSpec            responseSpec;

    @Spy ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private GeocodingService geocodingService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.header(anyString(), anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
    }

    @Test
    @DisplayName("geocode() — réponse Nominatim valide → retourne un Point JTS (lon=X, lat=Y)")
    void geocode_validResponse_returnsPoint() {
        String json = "[{\"lat\":\"48.8566\",\"lon\":\"2.3522\"}]";
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just(json));

        Point result = geocodingService.geocode("10 rue de Rivoli", "Paris", "75001", "France");

        assertThat(result).isNotNull();
        assertThat(result.getY()).isEqualTo(48.8566);
        assertThat(result.getX()).isEqualTo(2.3522);
    }

    @Test
    @DisplayName("geocode() — tableau JSON vide → retourne null")
    void geocode_emptyArray_returnsNull() {
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just("[]"));

        Point result = geocodingService.geocode("Rue inconnue", "Inconnue", "00000", "France");

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("geocode() — WebClient lance une exception → retourne null (exception avalée)")
    void geocode_webClientThrows_returnsNull() {
        when(responseSpec.bodyToMono(String.class))
                .thenReturn(Mono.error(new RuntimeException("timeout simulé")));

        Point result = geocodingService.geocode("1 rue Test", "Ville", "12345", "France");

        assertThat(result).isNull();
    }
}
