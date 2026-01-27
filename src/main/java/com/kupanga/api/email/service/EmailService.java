package com.kupanga.api.email.service;

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
     * Envoie un email de confirmation de création du compte utilisateur.
     *
     * <p>
     * Ce mail est généralement envoyé lors de la création d'un compte
     * </p>
     *
     * @param destinataire l'adresse email du destinataire
     */
    void SendWelcomeMessage(String destinataire);

    /**
     * Envoie un email de confirmation pour un nouveau compte utilisateur finalisé.
     *
     * <p>
     * Ce mail peut contenir les informations personnelles de l'utilisateur
     * ainsi que son email pour connexion.
     * </p>
     *
     * @param destinataire l'adresse email du destinataire
     * @param nom le nom de l'utilisateur
     * @param prenom le prénom de l'utilisateur
     * @param email l'adresse email de l'utilisateur pour la connexion
     */
    void sendWelcomeMessage(String destinataire, String nom, String prenom, String email);

    /**
     * Email de mise à jour du mot de passe.
     * @param destinataire le destinataire
     * @param resetLink le lien pour la mise à jour du mot de passe
     */
    void sendPasswordResetMail(String destinataire, String resetLink);

    /**
     * Email de confirmation de la mise à jour du mot de passe.
     * @param destinataire le destinataire
     */
    void sendPasswordUpdatedConfirmation(String destinataire);
}
