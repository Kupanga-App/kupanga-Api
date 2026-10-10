package com.kupanga.api.immobilier.service;

import com.kupanga.api.juridiction.Pays;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Point;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * B2 : le cache de géocodage (vrai {@code @Cacheable} Spring) distingue les adresses d'une même ville.
 * Le faux Nominatim renvoie une latitude qui dépend de la rue demandée.
 */
@SpringJUnitConfig(GeocodingCacheTest.Config.class)
@DisplayName("Tests — cache du géocodage (B2)")
class GeocodingCacheTest {

    private static final AtomicInteger APPELS_NOMINATIM = new AtomicInteger();

    @Configuration
    @EnableCaching
    static class Config {

        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager("geocode");
        }

        @Bean
        GeocodingService geocodingService() {
            WebClient fauxNominatim = WebClient.builder()
                    .exchangeFunction(requete -> {
                        APPELS_NOMINATIM.incrementAndGet();
                        String rue = UriComponentsBuilder.fromUri(requete.url()).build()
                                .getQueryParams().getFirst("street");
                        double lat = rue != null && rue.contains("Rivoli") ? 48.86 : 48.87;
                        return Mono.just(ClientResponse.create(HttpStatus.OK)
                                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                .body("[{\"lat\":\"" + lat + "\",\"lon\":\"2.35\"}]")
                                .build());
                    })
                    .build();
            return new GeocodingService(fauxNominatim, new ObjectMapper());
        }
    }

    @Autowired private GeocodingService geocodingService;
    @Autowired private CacheManager cacheManager;

    @BeforeEach
    void setUp() {
        cacheManager.getCache("geocode").clear();
        APPELS_NOMINATIM.set(0);
    }

    @Test
    @DisplayName("Deux adresses de la même ville et du même code postal → coordonnées distinctes")
    void memeVille_adressesDifferentes_coordonneesDistinctes() {
        Point rivoli = geocodingService.geocode("10 rue de Rivoli", "Paris", "75001", Pays.FR);
        Point louvre = geocodingService.geocode("2 rue du Louvre", "Paris", "75001", Pays.FR);

        assertThat(rivoli.getY()).isEqualTo(48.86);
        assertThat(louvre.getY()).isEqualTo(48.87);
        assertThat(APPELS_NOMINATIM).hasValue(2);
    }

    @Test
    @DisplayName("Même adresse (casse et espaces différents) → une seule requête Nominatim")
    void memeAdresse_casseEtEspaces_unSeulAppel() {
        geocodingService.geocode("10 rue de Rivoli", "Paris", "75001", Pays.FR);
        Point encore = geocodingService.geocode(" 10  RUE de rivoli ", "PARIS", "75001 ", Pays.FR);

        assertThat(encore.getY()).isEqualTo(48.86);
        assertThat(APPELS_NOMINATIM).hasValue(1);
    }

    @Test
    @DisplayName("Clé : voie, ville, code postal et pays normalisés")
    void cleCache() {
        assertThat(GeocodingService.cleCache(" 10  Rue de Rivoli ", "Paris", "75001", null))
                .isEqualTo("10 rue de rivoli|paris|75001|");
        assertThat(GeocodingService.cleCache("10 rue de Rivoli", "Paris", "75001", Pays.FR))
                .isNotEqualTo(GeocodingService.cleCache("2 rue du Louvre", "Paris", "75001", Pays.FR));
    }
}
