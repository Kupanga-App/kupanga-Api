package com.kupanga.api.juridiction;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

import java.util.EnumMap;
import java.util.Map;

/**
 * Profils de juridiction par pays ({@code kupanga.juridictions.FR}, {@code kupanga.juridictions.CD}…).
 * Un pays sans profil n'est pas pris en charge : aucun bien ne peut y être créé.
 * La cohérence des profils est contrôlée au démarrage par {@link JuridictionRegistry} ; une clé inconnue
 * (faute de frappe, ex. {@code champs-masque}) arrête aussi le démarrage.
 *
 * @param juridictions profil de chaque pays pris en charge
 * @param plafonds     C5 : montants maximaux par devise ({@code kupanga.plafonds.<DEVISE>})
 */
@ConfigurationProperties(prefix = "kupanga", ignoreUnknownFields = false)
public record JuridictionProperties(Map<Pays, ProfilJuridiction> juridictions,
                                    Map<Devise, PlafondsMontants> plafonds) {

    @ConstructorBinding
    public JuridictionProperties {
        juridictions = juridictions == null || juridictions.isEmpty()
                ? Map.of()
                : Map.copyOf(new EnumMap<>(juridictions));
        plafonds = plafonds == null || plafonds.isEmpty()
                ? Map.of()
                : Map.copyOf(new EnumMap<>(plafonds));
    }

    /** Profils seuls, sans plafonds (tests). */
    public JuridictionProperties(Map<Pays, ProfilJuridiction> juridictions) {
        this(juridictions, Map.of());
    }
}
