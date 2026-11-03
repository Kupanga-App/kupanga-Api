package com.kupanga.api.notification.enums;

public enum NotificationType {
    INVITATION_SIGNATURE_CONTRAT,
    CONTRAT_SIGNE,
    QUITTANCE_DISPONIBLE,
    INVITATION_SIGNATURE_EDL,
    EDL_SIGNE,
    BIEN_ASSIGNE,
    BIEN_ASSIGNATION_CONFIRMEE,
    /** Demande de location reçue par le locataire (referenceId = id de la demande). */
    DEMANDE_LOCATION,
    /** Le locataire a accepté : le bien lui est assigné (referenceId = id du bien). */
    DEMANDE_LOCATION_ACCEPTEE,
    /** Le locataire a refusé (referenceId = id du bien). */
    DEMANDE_LOCATION_REFUSEE,
    /** Le propriétaire a retiré sa demande (referenceId = id de la demande). */
    DEMANDE_LOCATION_ANNULEE
}
