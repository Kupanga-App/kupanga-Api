package com.kupanga.api.authentification.google;

public interface GoogleTokenVerifier {

    /**
     * Vérifie un ID token Google et retourne les informations de l'utilisateur.
     * @param idToken le token reçu du front-end
     * @return les informations extraites du token Google
     * @throws com.kupanga.api.exception.business.KupangaBusinessException si le token est invalide
     */
    GoogleUserInfo verify(String idToken);
}
