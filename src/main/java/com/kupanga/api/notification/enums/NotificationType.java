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
    /** Le locataire a accepté : le bien lui est réservé jusqu'à la signature du bail (referenceId = id du bien). */
    DEMANDE_LOCATION_ACCEPTEE,
    /** Le locataire a refusé (referenceId = id du bien). */
    DEMANDE_LOCATION_REFUSEE,
    /** Le propriétaire a retiré sa demande (referenceId = id de la demande). */
    DEMANDE_LOCATION_ANNULEE,
    /** Le candidat s'est désisté avant la signature du bail (au propriétaire ; referenceId = id du bien). */
    RESERVATION_DESISTEE,
    /** Le propriétaire a retiré la réservation (au candidat ; referenceId = id de la location). */
    RESERVATION_ANNULEE,
    /** 14 jours sans bail signé, au propriétaire (referenceId = id du bien). */
    RESERVATION_EXPIREE,
    /** 14 jours sans bail signé, au candidat (referenceId = id de la location). */
    RESERVATION_EXPIREE_CANDIDAT,
    /** Congé donné ou retiré par le locataire, au propriétaire (referenceId = id du bien). */
    CONGE_POUR_PROPRIETAIRE,
    /** Congé donné ou retiré par le propriétaire, au locataire (referenceId = id du bien). */
    CONGE_POUR_LOCATAIRE,
    /** Fin du bail enregistrée, au locataire (referenceId = id de la location). */
    LOCATION_TERMINEE
}
