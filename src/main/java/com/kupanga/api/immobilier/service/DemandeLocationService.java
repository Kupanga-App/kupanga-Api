package com.kupanga.api.immobilier.service;

import com.kupanga.api.immobilier.dto.readDTO.DemandeLocationDTO;

import java.time.Duration;
import java.util.List;

/**
 * Affectation d'un locataire avec son accord : le propriétaire envoie une demande à un candidat, le bien n'est
 * assigné qu'à l'acceptation.
 */
public interface DemandeLocationService {

    /** Durée de validité d'une demande (choix de l'utilisateur, 2026-11-03). */
    Duration VALIDITE = Duration.ofHours(72);

    /**
     * Le propriétaire propose son bien à un candidat (personne qui lui a écrit au sujet de ce bien).
     * 404 bien ou candidat introuvable (même réponse pour un compte inconnu), 403 bien d'autrui,
     * 409 bien archivé, déjà loué ou demande déjà en attente, 400 compte non locataire.
     */
    DemandeLocationDTO envoyer(String emailProprietaire, Long bienId, Long locataireId);

    /** Historique des demandes d'un bien (propriétaire seulement). */
    List<DemandeLocationDTO> duBien(String emailProprietaire, Long bienId);

    /** Le propriétaire retire une demande encore en attente (409 sinon). */
    void annuler(String emailProprietaire, Long demandeId);

    /** Demandes en attente reçues par le locataire connecté. */
    List<DemandeLocationDTO> recues(String emailLocataire);

    /** Le locataire accepte : le bien lui est assigné. 410 si la demande a expiré, 409 si le bien n'est plus libre. */
    DemandeLocationDTO accepter(String emailLocataire, Long demandeId);

    /** Le locataire refuse la demande. */
    DemandeLocationDTO refuser(String emailLocataire, Long demandeId);
}
