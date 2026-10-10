package com.kupanga.api.immobilier.research;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kupanga.api.immobilier.entity.PoiType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * B9 : une seule requête Overpass pour tous les types, délai borné, un seul nouvel essai sur erreur passagère,
 * et une réponse en échec ne devient jamais « aucun POI ».
 */
@DisplayName("Tests unitaires — PoiSearchService (B9)")
class PoiSearchServiceTest {

    private static final String QUATRE_COMPTES = """
            {"elements":[
              {"type":"count","id":0,"tags":{"nodes":"3","ways":"0","relations":"0","total":"3"}},
              {"type":"count","id":0,"tags":{"nodes":"1","ways":"0","relations":"0","total":"1"}},
              {"type":"count","id":0,"tags":{"nodes":"2","ways":"0","relations":"0","total":"2"}},
              {"type":"count","id":0,"tags":{"nodes":"0","ways":"0","relations":"0","total":"0"}}
            ]}""";

    private final AtomicInteger appels = new AtomicInteger();
    private final List<String> requetes = new ArrayList<>();
    private Point point;

    @BeforeEach
    void setUp() {
        point = new GeometryFactory(new PrecisionModel(), 4326).createPoint(new Coordinate(15.3136, -4.3317));
    }

    /** Faux Overpass : renvoie les réponses données, une par appel (la dernière se répète). */
    @SafeVarargs
    private PoiSearchService service(Mono<ClientResponse>... reponses) {
        WebClient fauxOverpass = WebClient.builder()
                .exchangeFunction(requete -> {
                    int i = appels.getAndIncrement();
                    requetes.add(requete.url().toString());
                    return reponses[Math.min(i, reponses.length - 1)];
                })
                .build();
        PoiSearchService service = new PoiSearchService(fauxOverpass, new ObjectMapper());
        service.attenteAvantNouvelEssai = Duration.ofMillis(1);
        return service;
    }

    private static Mono<ClientResponse> ok(String corps) {
        return Mono.just(ClientResponse.create(HttpStatus.OK)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(corps).build());
    }

    private static Mono<ClientResponse> erreur(HttpStatus statut) {
        return Mono.just(ClientResponse.create(statut).build());
    }

    @Test
    @DisplayName("Une seule requête pour les 4 types (out count), comptes lus dans l'ordre de PoiType")
    void uneSeuleRequete_comptesDansLOrdre() {
        Map<PoiType, Integer> resultat = service(ok(QUATRE_COMPTES)).calculerTousLesPoi(point);

        assertThat(resultat).containsExactlyInAnyOrderEntriesOf(Map.of(
                PoiType.SCHOOL, 3, PoiType.HOSPITAL, 1, PoiType.PHARMACY, 2, PoiType.KINDERGARTEN, 0));
        assertThat(appels).hasValue(1);

        String data = URLDecoder.decode(
                UriComponentsBuilder.fromUriString(requetes.get(0)).build(true).getQueryParams().getFirst("data"),
                StandardCharsets.UTF_8);
        assertThat(data).startsWith("[out:json][timeout:10];");
        assertThat(data.split("out count;", -1)).hasSize(PoiType.values().length + 1);
        assertThat(data).contains("node[\"amenity\"=\"school\"](around:5000.0,-4.331700,15.313600)");
    }

    @Test
    @DisplayName("Erreur passagère (503) puis succès → un seul nouvel essai")
    void erreurPassagere_unNouvelEssai() {
        Map<PoiType, Integer> resultat = service(erreur(HttpStatus.SERVICE_UNAVAILABLE), ok(QUATRE_COMPTES))
                .calculerTousLesPoi(point);

        assertThat(resultat).hasSize(4);
        assertThat(appels).hasValue(2);
    }

    @Test
    @DisplayName("Overpass en panne (503, 429) → 2 appels au plus, map vide (pas « aucun POI »)")
    void overpassEnPanne_deuxAppelsMax_mapVide() {
        assertThat(service(erreur(HttpStatus.SERVICE_UNAVAILABLE)).calculerTousLesPoi(point)).isEmpty();
        assertThat(appels).hasValue(2);

        appels.set(0);
        assertThat(service(erreur(HttpStatus.TOO_MANY_REQUESTS)).calculerTousLesPoi(point)).isEmpty();
        assertThat(appels).hasValue(2);
    }

    @Test
    @DisplayName("Requête refusée (400) → pas de nouvel essai, map vide")
    void requeteRefusee_pasDeNouvelEssai() {
        assertThat(service(erreur(HttpStatus.BAD_REQUEST)).calculerTousLesPoi(point)).isEmpty();
        assertThat(appels).hasValue(1);
    }

    @Test
    @DisplayName("Pas de réponse dans le délai → abandon rapide (2 essais), map vide")
    void delaiDepasse_abandon() {
        PoiSearchService service = service(Mono.never());
        service.delaiMax = Duration.ofMillis(100);

        long debut = System.nanoTime();
        assertThat(service.calculerTousLesPoi(point)).isEmpty();

        assertThat(appels).hasValue(2);
        assertThat(Duration.ofNanos(System.nanoTime() - debut)).isLessThan(Duration.ofSeconds(5));
    }

    @Test
    @DisplayName("Réponse partielle (remark d'erreur Overpass ou comptes manquants) → map vide")
    void reponsePartielle_mapVide() {
        String avecRemarque = QUATRE_COMPTES.replace("{\"elements\"",
                "{\"remark\":\"runtime error: Query timed out\",\"elements\"");
        String troisComptes = """
                {"elements":[
                  {"type":"count","tags":{"nodes":"3"}},{"type":"count","tags":{"nodes":"1"}},{"type":"count","tags":{"nodes":"2"}}
                ]}""";

        assertThat(service(ok(avecRemarque)).calculerTousLesPoi(point)).isEmpty();
        assertThat(service(ok(troisComptes)).calculerTousLesPoi(point)).isEmpty();
        assertThat(service(ok("pas du json")).calculerTousLesPoi(point)).isEmpty();
    }

    @Test
    @DisplayName("Clé de cache : position arrondie à 4 décimales")
    void cleCache_arrondie() {
        Point voisin = new GeometryFactory(new PrecisionModel(), 4326).createPoint(new Coordinate(15.31362, -4.33171));

        assertThat(PoiSearchService.cleCache(point)).isEqualTo("-4.3317:15.3136")
                .isEqualTo(PoiSearchService.cleCache(voisin));
    }
}
