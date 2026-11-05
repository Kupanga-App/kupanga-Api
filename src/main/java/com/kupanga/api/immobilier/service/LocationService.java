package com.kupanga.api.immobilier.service;

import com.kupanga.api.immobilier.dto.readDTO.LocationDTO;
import com.kupanga.api.immobilier.entity.Bien;
import com.kupanga.api.immobilier.entity.Contrat;
import com.kupanga.api.immobilier.entity.DemandeLocation;
import com.kupanga.api.immobilier.entity.Location;
import com.kupanga.api.user.entity.User;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Locations (décision du 2026-11-05) : accepter une demande RÉSERVE le bien pendant {@link #DUREE_RESERVATION} ;
 * le candidat devient locataire à la signature du bail ; avant, il peut se désister et le propriétaire retirer la
 * réservation. Pendant la réservation, l'annonce est retirée de la recherche.
 */
public interface LocationService {

    /** Durée d'une réservation sans bail signé (choix de l'utilisateur, 2026-11-05). */
    Duration DUREE_RESERVATION = Duration.ofDays(14);

    // ─── Usage interne (autres services) ─────────────────────────────────────

    /** 409 si le bien a déjà un locataire ou une réservation en cours (fait expirer les réservations échues). */
    void verifierBienLibre(Bien bien, LocalDateTime maintenant);

    /** Réserve le bien pour le candidat de la demande acceptée (409 si le bien n'est plus libre). */
    Location reserver(DemandeLocation demande, LocalDateTime maintenant);

    /** Réservation encore valable du bien, s'il y en a une. */
    Optional<Location> reservationValable(Long bienId);

    /**
     * Locataire d'un nouveau bail : le locataire actuel du bien, ou le candidat qui l'a réservé.
     * 400 si l'adresse ne correspond à aucun des deux.
     */
    User locataireDuBail(Bien bien, String emailLocataire);

    /**
     * Un bail d'un candidat (et non du locataire actuel) ne se signe que tant que sa réservation est valable :
     * 409 sinon (désistement, retrait ou expiration).
     */
    void verifierBailSignable(Contrat contrat);

    /** Bail signé par les deux parties : la réservation du candidat devient une location active. */
    void activerApresSignature(Contrat contrat);

    /** Tâche planifiée : passe en EXPIREE les réservations échues, annule leurs baux non signés, prévient les parties. */
    int expirerReservationsEchues();

    // ─── Routes ──────────────────────────────────────────────────────────────

    /** Location réservée ou en cours d'un bien (propriétaire seulement : 404 / 403). */
    Optional<LocationDTO> enCoursDuBien(String emailProprietaire, Long bienId);

    /** Réservations encore valables du compte connecté (candidat). */
    List<LocationDTO> mesReservations(String email);

    /** Le candidat se désiste avant la signature du bail (404 si ce n'est pas la sienne, 409 si plus réservée). */
    LocationDTO desister(String email, Long locationId);

    // ─── Fin de bail (2026-11-05) ────────────────────────────────────────

    /** Durée maximale entre le congé et la date de fin annoncée. */
    java.time.Period CONGE_MAXIMAL = java.time.Period.ofYears(2);

    /**
     * Le propriétaire ou le locataire de la location active donne congé pour une date (aujourd'hui au plus tôt).
     * 404 si le compte n'est partie à aucune location de ce numéro, 409 si elle n'est pas active ou si un congé
     * est déjà donné, 400 si la date est hors bornes.
     */
    LocationDTO donnerConge(String email, Long locationId, java.time.LocalDate dateFin);

    /** Celui qui a donné congé le retire (404 / 409 sinon). */
    LocationDTO retirerConge(String email, Long locationId);

    /**
     * Le propriétaire enregistre la fin du bail, à partir de la date annoncée par le congé, avec le dépôt
     * restitué : location TERMINEE, bien libéré (de nouveau dans les annonces), baux non signés du locataire
     * annulés. 404 si ce n'est pas sa location, 409 sans congé ou avant la date, 400 si le montant dépasse le
     * dépôt ou si le motif de la retenue manque.
     */
    LocationDTO terminer(String emailProprietaire, Long locationId, java.math.BigDecimal depotRestitue,
                         String motifRetenue);

    /** Le propriétaire retire la réservation avant la signature du bail (404 si pas la sienne, 409 si plus réservée). */
    LocationDTO annuler(String emailProprietaire, Long locationId);
}
