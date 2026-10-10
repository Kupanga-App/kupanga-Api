package com.kupanga.api.immobilier.research;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kupanga.api.immobilier.entity.PoiType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Point;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

import static com.kupanga.api.immobilier.constant.Constant.RAYON_DEFAUT;

/**
 * Recherche des POI autour d'un bien via Overpass (OpenStreetMap).
 * B9 : une seule requête pour tous les types ({@code out count;} par type), délai de 10 s, un seul nouvel essai,
 * résultat mis en cache par position arrondie. Avant : une requête par type, 20 s × 10 essais chacune.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PoiSearchService {

    private static final PoiType[] TYPES = PoiType.values();

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    // Non finals : ajustés par les tests
    Duration delaiMax = Duration.ofSeconds(10);
    Duration attenteAvantNouvelEssai = Duration.ofSeconds(2);

    /**
     * Compte les POI de chaque type autour d'un point.
     *
     * @return map {@code PoiType → nombre trouvé}, <b>vide</b> si Overpass n'a pas répondu correctement :
     *         « inconnu » n'est pas « aucun POI », et un résultat vide n'est pas mis en cache
     */
    @Cacheable(
            value  = "poi",
            key    = "T(com.kupanga.api.immobilier.research.PoiSearchService).cleCache(#localisation)",
            unless = "#result.isEmpty()"
    )
    public Map<PoiType, Integer> calculerTousLesPoi(Point localisation) {
        try {
            String reponse = callOverpass(buildQuery(localisation));
            Map<PoiType, Integer> resultats = lireComptes(reponse);
            log.debug("POI calculés : {}", resultats);
            return resultats;
        } catch (Exception e) {
            log.warn("Recherche des POI impossible : {}", e.getClass().getSimpleName());
            return Map.of();
        }
    }

    /** Clé de cache : position arrondie à 4 décimales (~11 m), le rayon de recherche étant fixe. */
    public static String cleCache(Point localisation) {
        return String.format(Locale.ROOT, "%.4f:%.4f", localisation.getY(), localisation.getX());
    }

    private String callOverpass(String query) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .scheme("https")
                        .host("overpass-api.de")
                        .path("/api/interpreter")
                        .queryParam("data", "{data}")
                        .build(query))
                .header(HttpHeaders.USER_AGENT, "KupangaImmobilier/1.0")
                .retrieve()
                .bodyToMono(String.class)
                .timeout(delaiMax)
                .retryWhen(Retry.backoff(1, attenteAvantNouvelEssai).filter(PoiSearchService::reessayable))
                .block();
    }

    /** Nouvel essai seulement sur une erreur passagère : délai, réseau, 429 ou 5xx (pas sur une requête refusée). */
    private static boolean reessayable(Throwable erreur) {
        if (erreur instanceof WebClientResponseException reponse) {
            return reponse.getStatusCode().is5xxServerError() || reponse.getStatusCode().value() == 429;
        }
        return true;
    }

    /**
     * Une instruction {@code out count;} par type, dans l'ordre de {@link PoiType} : Overpass renvoie un élément
     * {@code count} par instruction, dans le même ordre.
     */
    private Map<PoiType, Integer> lireComptes(String reponse) throws Exception {
        JsonNode json = objectMapper.readTree(reponse);
        JsonNode elements = json.path("elements");
        // "remark" : erreur d'exécution côté Overpass (délai dépassé, mémoire) avec un résultat partiel
        if (json.has("remark") || !elements.isArray() || elements.size() != TYPES.length) {
            log.warn("Réponse Overpass incomplète : {} compte(s) sur {}", elements.size(), TYPES.length);
            return Map.of();
        }
        Map<PoiType, Integer> resultats = new EnumMap<>(PoiType.class);
        for (int i = 0; i < TYPES.length; i++) {
            JsonNode tags = elements.get(i).path("tags");
            if (!tags.has("nodes")) return Map.of();
            resultats.put(TYPES[i], tags.path("nodes").asInt(0));
        }
        return resultats;
    }

    /**
     * Requête Overpass QL : un {@code out count;} par type de POI autour du point.
     *
     * @param location la position du bien (lat/lon extraits du Point PostGIS)
     */
    String buildQuery(Point location) {
        double lat = location.getY();
        double lon = location.getX();

        StringBuilder query = new StringBuilder("[out:json][timeout:10];");
        for (PoiType poi : TYPES) {
            query.append(String.format(
                    Locale.US,   // ← force le point comme séparateur décimal
                    "node[\"%s\"=\"%s\"](around:%.1f,%.6f,%.6f);out count;",
                    poi.getOsmKey(), poi.getOsmValue(), RAYON_DEFAUT, lat, lon));
        }
        return query.toString();
    }
}
