package com.kupanga.api.email.service;

import com.kupanga.api.immobilier.entity.Contrat;
import com.kupanga.api.immobilier.entity.DemandeLocation;
import com.kupanga.api.immobilier.entity.EtatDesLieux;
import com.kupanga.api.immobilier.entity.Quittance;

/**
 * Service pour l'envoi d'emails liés aux utilisateurs.
 * Couvre la gestion des comptes, les contrats, états des lieux et quittances.
 */
public interface EmailService {

    /**
     * Envoie un email de bienvenue pour un nouveau compte utilisateur finalisé.
     *
     * @param destinataire l'adresse email du destinataire
     * @param prenom       le prénom de l'utilisateur
     */
    void sendWelcomeMessage(String destinataire, String prenom);

    /**
     * A14 : envoie le lien de confirmation de l'adresse e-mail (après le commit, B11).
     *
     * @param destinataire l'adresse e-mail à confirmer
     * @param prenom       le prénom de l'utilisateur
     * @param token        le jeton du lien (valable 24 h)
     */
    void envoyerVerificationEmail(String destinataire, String prenom, String token);

    /**
     * A14 : prévient le titulaire d'un compte déjà vérifié qu'une inscription a été tentée avec son adresse
     * (l'API répond comme pour une nouvelle inscription : pas d'énumération des comptes).
     *
     * @param destinataire l'adresse du compte existant
     */
    void envoyerTentativeInscription(String destinataire);

    /**
     * Email de mise à jour du mot de passe.
     * @param destinataire le destinataire
     */
    void sendPasswordResetMail(String destinataire,  String resetToken);

    /**
     * Email de confirmation de la mise à jour du mot de passe.
     * @param destinataire le destinataire
     */
    void sendPasswordUpdatedConfirmation(String destinataire);

    /**
     * Envoi email d'invitation à signé
     * @param contrat le Contrat
     * @param token le token
     */
    void envoyerInvitationSignature(Contrat contrat, String token);

    /**
     * Envoie email de confirmation de la signature.
     * @param contrat le contrat.
     */
    void envoyerConfirmationContratSigne(Contrat contrat);

    /**
     * Envoie un email d'invitation au locataire pour signer l'état des lieux.
     *
     * @param edl   l'état des lieux
     * @param token le token de signature (unique, 72h)
     */
    void envoyerInvitationSignature(EtatDesLieux edl, String token);

    /**
     * Envoie un email de confirmation aux deux parties une fois l'EDL signé,
     * avec le PDF en pièce jointe.
     *
     * @param edl l'état des lieux signé
     */
    void envoyerConfirmationEdlSigne(EtatDesLieux edl);


    /**
     * Envoie la quittance de loyer au locataire avec le PDF en pièce jointe.
     * Appelé automatiquement lors du marquage d'une quittance comme payée.
     *
     * @param quittance la quittance payée
     */
    void envoyerQuittance(Quittance quittance);


    /**
     * Prévient le locataire qu'un propriétaire lui propose son logement (lien vers ses demandes reçues,
     * valable autant que la demande).
     */
    void envoyerDemandeLocation(DemandeLocation demande);

    /**
     * Fin d'une réservation avant la signature du bail (décision du 2026-11-05) : prévient le destinataire
     * (propriétaire ou candidat) du désistement, du retrait ou de l'expiration.
     *
     * @param motif {@code DESISTEE}, {@code ANNULEE} ou {@code EXPIREE}
     */
    /** Fin de bail : prévient l'autre partie d'un congé donné ou retiré. */
    void envoyerConge(com.kupanga.api.immobilier.entity.Location location, com.kupanga.api.user.entity.User destinataire,
                      boolean retire);

    /** Fin de bail enregistrée : prévient le locataire, avec le dépôt restitué et le motif d'une retenue. */
    void envoyerFinDeBail(com.kupanga.api.immobilier.entity.Location location);

    void envoyerFinReservation(com.kupanga.api.immobilier.entity.Location location,
                               com.kupanga.api.immobilier.entity.StatutLocation motif,
                               com.kupanga.api.user.entity.User destinataire);
}
