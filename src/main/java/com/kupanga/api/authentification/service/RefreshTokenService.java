package com.kupanga.api.authentification.service;

import com.kupanga.api.authentification.entity.RefreshToken;
import com.kupanga.api.user.entity.User;

public interface RefreshTokenService {

    /**
     * Permet de créer un refresh token.
     * @param user utilisateur
     * @return le token
     */
    String createRefreshToken(User user) ;

    /**
     * Renvoie le refresh token
     * @param token token
     * @return refresh token
     */
    RefreshToken getByToken(String token) ;

    /**
     * Supprime le refresh token de la base de données.
     *
     * @param token le refresh token à supprimer
     */
    void deleteRefreshToken(String token);
}
