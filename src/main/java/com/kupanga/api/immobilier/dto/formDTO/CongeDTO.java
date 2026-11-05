package com.kupanga.api.immobilier.dto.formDTO;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * Fin de bail : congé donné par le propriétaire ou le locataire (2026-11-05).
 *
 * @param dateFin dernier jour de la location (aujourd'hui au plus tôt, dans les 2 ans). Aucun préavis légal n'est
 *                imposé par l'application : il dépend du pays et du bail (à faire valider avec le modèle de bail).
 */
public record CongeDTO(
        @NotNull(message = "La date de fin est obligatoire")
        LocalDate dateFin
) {
}
