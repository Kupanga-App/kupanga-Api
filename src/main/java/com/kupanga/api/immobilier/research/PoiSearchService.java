package com.kupanga.api.immobilier.research;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kupanga.api.immobilier.entity.PoiType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Point;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.EnumMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class PoiSearchService {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    private static final String OVERPASS_URL = "https://overpass-api.de/api/interpreter";
    private static final double RAYON_DEFAUT  = 500.0; // 500 mètres

    /**
     * Calcule la présence de TOUS les POI pour un bien en un seul appel par type.
     * Retourne une map PoiType → nombre trouvé.
     */
    public Map<PoiType, Integer> calculerTousLesPoi(Point localisation) {
        Map<PoiType, Integer> resultats = new EnumMap<>(PoiType.class);

        for (PoiType poi : PoiType.values()) {
            int nombre = compterPoi(localisation, poi, RAYON_DEFAUT);
            resultats.put(poi, nombre);
            log.info("POI {} : {} trouvé(s)", poi.getLabelFr(), nombre);
        }

        return resultats;
    }

    /**
     * Compte le nombre de POI d'un type donné autour d'un point.
     */
    private int compterPoi(Point location, PoiType poi, double rayonMetres) {
        try {
            String query    = buildQuery(location, poi, rayonMetres);
            String response = callOverpass(query);
            JsonNode json   = objectMapper.readTree(response);

            if (json.has("elements")) {
                return json.get("elements").size();
            }
        } catch (Exception e) {
            log.warn("Erreur POI {} : {}", poi.getLabelFr(), e.getMessage());
        }
        return 0;
    }

    private String callOverpass(String query) {
        return webClient.post()
                .uri(OVERPASS_URL)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue("data=" + query)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(10))
                .block();
    }

    private String buildQuery(Point location, PoiType poi, double rayonMetres) {
        double lat = location.getY();
        double lon = location.getX();
        return String.format(
                "[out:json];node[\"%s\"=\"%s\"](around:%.1f,%.6f,%.6f);out count;",
                poi.getOsmKey(), poi.getOsmValue(), rayonMetres, lat, lon
        );
        // "out count" → retourne seulement le nombre, pas les données complètes
        // beaucoup plus rapide que "out;"
    }
}
