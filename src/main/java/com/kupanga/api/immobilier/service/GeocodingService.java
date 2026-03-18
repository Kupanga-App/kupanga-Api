package com.kupanga.api.immobilier.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;
import java.util.Optional;

/**
 * Service de géocodage des adresses en coordonnées GPS.
 * Utilise l'API Nominatim d'OpenStreetMap pour récupérer les coordonnées (latitude, longitude)
 * à partir d'une adresse complète. Les résultats sont mis en cache avec Redis pour améliorer
 * les performances et éviter de dépasser les limites de requêtes de l'API externe.
 * Exemple d'utilisation :
 * Optional<Point> point = geocodingService.geocode("10 rue de Rivoli", "Paris", "75001");
 * Notes – Le cache Redis utilise comme clé : "ville : codePostal"
 * . Le type Point est compatible avec PostGIS (SRID 4326)
 */
@Service
@RequiredArgsConstructor // Génère un constructeur avec tous les champs final pour l'injection
@Slf4j                   // Fournit un logger 'log' pour cette classe
public class GeocodingService {

    private final WebClient webClient;       // Client HTTP réactif pour appeler Nominatim
    private final ObjectMapper objectMapper; // Pour parser la réponse JSON en objets Java

    /**
     * Géocode une adresse complète en renvoyant un Point JTS (longitude / latitude).
     * Les résultats sont mis en cache avec Redis pour éviter les appels répétitifs.
     *
     * @param adresse   Rue ou numéro de voie
     * @param ville     Ville
     * @param codePostal Code postal
     * @return Optional<Point> contenant la position GPS si disponible
     */
    // Cache le Point directement — plus simple à sérialiser pour Redis
    @Cacheable(
            value  = "geocode",
            key    = "#ville + ':' + #codePostal",
            unless = "#result == null"
    )
    @Nullable
    public Point geocode(String adresse, String ville, String codePostal, String pays) {
        try {
            String url      = buildUrl(adresse, ville, codePostal, pays);
            String response = webClient.get()
                    .uri(url)
                    .header("User-Agent", "KupangaImmobilier/1.0")
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(5))
                    .block();

            return parseResponse(response).orElse(null);

        } catch (Exception e) {
            log.warn("Échec géocodage pour {} {} : {}", ville, codePostal, e.getMessage());
            return null;
        }
    }

    /**
     * Parse la réponse JSON de Nominatim pour créer un Point JTS.
     *
     * @param response JSON brut de l'API Nominatim
     * @return Optional<Point> si la réponse contient des coordonnées valides
     * @throws Exception en cas d'erreur de parsing
     */
    private Optional<Point> parseResponse(String response) throws Exception {
        JsonNode json = objectMapper.readTree(response);

        // Vérifie si la réponse est un tableau non vide
        if (!json.isArray() || json.isEmpty()) return Optional.empty();

        // Prend le premier résultat (meilleure correspondance)
        JsonNode first = json.get(0);
        double lat = first.get("lat").asDouble();
        double lon = first.get("lon").asDouble();

        // Création du Point PostGIS (SRID 4326 = WGS84 standard GPS)
        GeometryFactory factory = new GeometryFactory(new PrecisionModel(), 4326);
        Coordinate coord = new Coordinate(lon, lat); // PostGIS = X=longitude, Y=latitude
        return Optional.of(factory.createPoint(coord));
    }

    /**
     * Construit l'URL de l'API Nominatim avec tous les paramètres requis.
     * Les entrées utilisateur sont sanitizées pour éviter injections et abus.
     *
     * @param adresse Rue / numéro
     * @param ville   Ville
     * @param cp      Code postal
     * @param pays    Pays
     * @return URL complète encodée
     */
    private String buildUrl(String adresse, String ville, String cp, String pays) {
        return UriComponentsBuilder
                .fromHttpUrl("https://nominatim.openstreetmap.org/search")
                .queryParam("street", sanitize(adresse))
                .queryParam("city", sanitize(ville))
                .queryParam("postalcode", sanitize(cp))
                .queryParam("country", sanitize(pays))
                .queryParam("format", "json")            // Format de sortie JSON
                .queryParam("limit", "1")                // Limite à 1 résultat
                .queryParam("addressdetails", "1")       // Inclut les détails d'adresse
                .build()
                .toUriString();
    }

    /**
     * Nettoie une entrée utilisateur pour prévenir les injections et caractères dangereux.
     *
     * @param input Chaîne brute
     * @return Chaîne nettoyée et tronquée si nécessaire
     */
    private String sanitize(String input) {
        if (input == null) return "";

        String cleaned = input
                .replaceAll("(?i)https?://\\S+", "")   // Supprime URLs
                .replaceAll("[\\r\\n\\t]", "")         // Supprime CRLF
                .replaceAll("[<>\"'{}|\\\\^`]", "")    // Supprime caractères spéciaux
                .trim();

        // Limite la longueur pour éviter les abus
        return cleaned.length() > 150 ? cleaned.substring(0, 150) : cleaned;
    }
}