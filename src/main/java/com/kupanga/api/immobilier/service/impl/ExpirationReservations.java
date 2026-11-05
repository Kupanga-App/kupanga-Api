package com.kupanga.api.immobilier.service.impl;

import com.kupanga.api.immobilier.service.LocationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Passe en EXPIREE les réservations sans bail signé après 14 jours (décision du 2026-11-05), annule leurs baux
 * non signés et prévient les deux parties. Toutes les heures par défaut ; planification désactivée en test
 * ({@code PlanificationConfig}). Entre deux passages, le statut affiché est déjà juste ({@code Location.statutA})
 * et un bail ne peut plus être signé ({@code LocationService.verifierBailSignable}).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ExpirationReservations {

    private final LocationService locationService;

    /** Cron Spring (6 champs) ; jamais le format court des durées (cf. Décisions 2026-11-02). */
    @Scheduled(cron = "${locations.expiration.cron:0 7 * * * *}")
    public void expirer() {
        try {
            locationService.expirerReservationsEchues();
        } catch (RuntimeException e) {
            log.error("Expiration des réservations en échec ({})", e.getClass().getSimpleName());
        }
    }
}
