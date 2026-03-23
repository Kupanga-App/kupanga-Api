package com.kupanga.api.email.service;

import com.kupanga.api.immobilier.entity.Contrat;
import com.kupanga.api.immobilier.entity.EtatDesLieux;
import com.kupanga.api.immobilier.entity.Quittance;

/**
 * Service pour l'envoi d'emails liés aux utilisateurs.
 *
 * <p>
 * Cette interface définit les opérations d'envoi de mails pour la gestion des comptes utilisateurs,
 * telles que l'envoi d'un mot de passe temporaire ou la confirmation de création de compte.
 * </p>
 */
public interface EmailService {


    /**
     * Envoie un email de confirmation pour un nouveau compte utilisateur finalisé.
     *
     * <p>
     * Ce mail peut contenir les informations personnelles de l'utilisateur
     * ainsi que son email pour connexion.
     * </p>
     *
     * @param destinataire l'adresse email du destinataire
     * @param prenom le prénom de l'utilisateur
     */
    void sendWelcomeMessage(String destinataire, String prenom);

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

}
