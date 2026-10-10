package com.kupanga.api.immobilier.research;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
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
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/** B9 : vrai {@code @Cacheable} — un même emplacement n'interroge Overpass qu'une fois, un échec n'est pas gardé. */
@SpringJUnitConfig(PoiCacheTest.Config.class)
@DisplayName("Tests — cache des POI (B9)")
class PoiCacheTest {

    private static final AtomicInteger APPELS_OVERPASS = new AtomicInteger();
    private static final AtomicBoolean OVERPASS_EN_PANNE = new AtomicBoolean();

    @Configuration
    @EnableCaching
    static class Config {

        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager("poi");
        }

        @Bean
        PoiSearchService poiSearchService() {
            WebClient fauxOverpass = WebClient.builder()
                    .exchangeFunction(requete -> {
                        APPELS_OVERPASS.incrementAndGet();
                        if (OVERPASS_EN_PANNE.get()) {
                            return Mono.just(ClientResponse.create(HttpStatus.BAD_REQUEST).build());
                        }
                        String compte = "{\"type\":\"count\",\"tags\":{\"nodes\":\"1\"}}";
                        return Mono.just(ClientResponse.create(HttpStatus.OK)
                                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                .body("{\"elements\":[" + String.join(",", compte, compte, compte, compte) + "]}")
                                .build());
                    })
                    .build();
            return new PoiSearchService(fauxOverpass, new ObjectMapper());
        }
    }

    @Autowired private PoiSearchService poiSearchService;
    @Autowired private CacheManager cacheManager;

    private final GeometryFactory geometrie = new GeometryFactory(new PrecisionModel(), 4326);

    @BeforeEach
    void setUp() {
        APPELS_OVERPASS.set(0);
        OVERPASS_EN_PANNE.set(false);
        cacheManager.getCache("poi").clear();
    }

    @Test
    @DisplayName("Deux biens au même endroit (à quelques mètres) → un seul appel à Overpass")
    void memeEndroit_unSeulAppel() {
        poiSearchService.calculerTousLesPoi(geometrie.createPoint(new Coordinate(15.31360, -4.33170)));
        poiSearchService.calculerTousLesPoi(geometrie.createPoint(new Coordinate(15.31362, -4.33171)));

        assertThat(APPELS_OVERPASS).hasValue(1);
    }

    @Test
    @DisplayName("Échec d'Overpass non mis en cache : l'appel suivant réessaie")
    void echec_nonMisEnCache() {
        Point point = geometrie.createPoint(new Coordinate(2.3522, 48.8566));
        OVERPASS_EN_PANNE.set(true);
        assertThat(poiSearchService.calculerTousLesPoi(point)).isEmpty();

        OVERPASS_EN_PANNE.set(false);
        assertThat(poiSearchService.calculerTousLesPoi(point)).hasSize(4);
        assertThat(APPELS_OVERPASS).hasValue(2);
    }
}
