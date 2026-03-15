package com.kupanga.api.immobilier.research;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kupanga.api.immobilier.entity.PoiType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Point;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

import static com.kupanga.api.immobilier.constant.Constant.RAYON_DEFAUT;

@Service
@RequiredArgsConstructor
@Slf4j
public class PoiSearchService {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    /**
     * Calcule la présence de TOUS les POI pour un bien en un appel par type.
     * Retourne une map PoiType → nombre trouvé.
     */
    public Map<PoiType, Integer> calculerTousLesPoi(Point localisation) {
        Map<PoiType, Integer> resultats = new EnumMap<>(PoiType.class);

        for (PoiType poi : PoiType.values()) {
            int nombre = compterPoi(localisation, poi);
            resultats.put(poi, nombre);
            log.info("POI {} : {} trouvé(s)", poi.getLabelFr(), nombre);
        }

        return resultats;
    }

    /**
     * Compte le nombre de POI d'un type donné autour d'un point.
     *
     * @param location la position du bien
     * @param poi      le type de POI à rechercher
     * @return le nombre de POI trouvés, 0 en cas d'erreur
     */
    private int compterPoi(Point location, PoiType poi) {
        try {
            String query = buildQuery(location, poi);
            log.info(">>> Query [{}] : {}", poi.getLabelFr(), query);  // ← log INFO pour voir en console

            String response = callOverpass(query);
            JsonNode json   = objectMapper.readTree(response);

            // "out;" retourne un tableau "elements" dont la taille = nombre de POI trouvés
            if (json.has("elements") && json.get("elements").isArray()) {
                return json.get("elements").size();
            }

        } catch (Exception e) {
            log.warn("Erreur POI {} : {}", poi.getLabelFr(), e.getMessage());
        }
        return 0;
    }

    /**
     * Envoie la requête Overpass QL via GET avec encodage correct du body.
     *
     * @param query la requête Overpass QL
     * @return la réponse JSON brute
     */
    private String callOverpass(String query) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .scheme("https")
                        .host("overpass-api.de")
                        .path("/api/interpreter")
                        .queryParam("data", query)
                        .build())
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(20))
                .retry(10)   // ← réessaie 10 fois en cas d'erreur
                .block();
    }

    /**
     * Construit la requête Overpass QL pour rechercher un POI autour d'un point.
     * Utilise "out;" pour retourner les éléments complets — parsing simple via .size()
     *
     * @param location la position du bien (lat/lon extraits du Point PostGIS)
     * @param poi      le type de POI avec sa clé et valeur OSM
     * @return la requête Overpass QL formatée
     */
    private String buildQuery(Point location, PoiType poi) {
        double lat = location.getY();
        double lon = location.getX();

        return String.format(
                Locale.US,   // ← force le point comme séparateur décimal
                "[out:json];node[\"%s\"=\"%s\"](around:%.1f,%.6f,%.6f);out;",
                poi.getOsmKey(),
                poi.getOsmValue(),
                RAYON_DEFAUT,
                lat,
                lon
        );
    }
}