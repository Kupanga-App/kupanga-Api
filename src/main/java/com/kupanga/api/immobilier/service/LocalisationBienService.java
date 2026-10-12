package com.kupanga.api.immobilier.service;

import com.kupanga.api.immobilier.entity.PrecisionLocalisation;
import com.kupanga.api.juridiction.Pays;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * C2 / B10 : position d'un bien sans jamais bloquer sa création. Ordre : repère du propriétaire (carte ou GPS),
 * adresse géocodée, centre du quartier, de la commune, de la ville, sinon aucune position.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LocalisationBienService {

    private static final GeometryFactory WGS84 = new GeometryFactory(new PrecisionModel(), 4326);

    private final GeocodingService geocodingService;

    /** Position retenue et son origine ; {@code point} est {@code null} si {@code precision} vaut AUCUNE. */
    public record Localisation(@Nullable Point point, PrecisionLocalisation precision) {
    }

    /** Point WGS84 d'un repère déjà validé (bornes contrôlées par le DTO). */
    public static Point repere(double latitude, double longitude) {
        return WGS84.createPoint(new Coordinate(longitude, latitude)); // PostGIS : X = longitude, Y = latitude
    }

    public Localisation localiser(@Nullable Double latitude, @Nullable Double longitude,
                                  String adresse, String ville, @Nullable String codePostal,
                                  @Nullable String quartier, @Nullable String commune, Pays pays) {
        if (latitude != null && longitude != null) {
            return new Localisation(repere(latitude, longitude), PrecisionLocalisation.REPERE);
        }
        Point point = geocodingService.geocode(adresse, ville, codePostal, pays);
        if (point != null) {
            return new Localisation(point, PrecisionLocalisation.ADRESSE);
        }
        if (renseigne(quartier)) {
            point = geocodingService.geocodeZone(List.of(quartier, nonNull(commune), ville), pays);
            if (point != null) return new Localisation(point, PrecisionLocalisation.QUARTIER);
        }
        if (renseigne(commune)) {
            point = geocodingService.geocodeZone(List.of(commune, ville), pays);
            if (point != null) return new Localisation(point, PrecisionLocalisation.COMMUNE);
        }
        point = geocodingService.geocodeZone(List.of(ville), pays);
        if (point != null) {
            return new Localisation(point, PrecisionLocalisation.VILLE);
        }
        log.warn("Aucune position trouvée pour un bien à créer ({}) : créé sans position", pays);
        return new Localisation(null, PrecisionLocalisation.AUCUNE);
    }

    private static boolean renseigne(String valeur) {
        return valeur != null && !valeur.isBlank();
    }

    private static String nonNull(String valeur) {
        return valeur == null ? "" : valeur;
    }
}
