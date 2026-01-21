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
     * Envoie un email contenant un mot de passe temporaire à un utilisateur.
     *
     * <p>
     * Ce mail est généralement envoyé lors de la création d'un compte ou
     * d'une réinitialisation de mot de passe.
     * </p>
     *
     * @param destinataire l'adresse email du destinataire
     * @param password le mot de passe temporaire à communiquer
     */
    void envoyerMailMotDePasseTemporaire(String destinataire, String password);

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
    void envoyerMailNouveauCompteFinaliser(String destinataire, String nom, String prenom, String email);
}
