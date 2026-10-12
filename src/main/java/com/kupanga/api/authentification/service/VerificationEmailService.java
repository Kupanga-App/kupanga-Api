package com.kupanga.api.authentification.service;

import com.kupanga.api.user.entity.User;

/** A14 : vérification de l'adresse e-mail des comptes créés par inscription. */
public interface VerificationEmailService {

    /** Crée un jeton (remplace le précédent) et envoie le lien de vérification après le commit. */
    void envoyerLien(User user);

    /**
     * Valide le jeton : le compte est marqué vérifié, le jeton supprimé, l'e-mail de bienvenue envoyé.
     *
     * @return message de confirmation
     */
    String verifier(String token);

    /** Supprime le lien en attente du compte, s'il y en a un. */
    void annulerLien(Long userId);

    /** Renvoie un lien si le compte existe et n'est pas vérifié ; même réponse dans tous les cas. */
    String renvoyer(String email);
}
