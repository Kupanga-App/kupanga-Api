package com.kupanga.api.authentification.service;

import com.kupanga.api.authentification.entity.PasswordResetToken;

public interface PasswordResetTokenService {

    /**
     * Récupère PasswordResetToken à partir du token
     * @param token le token
     * @return PasswordResetToken
     */
    PasswordResetToken getByToken(String token);

    /**
     * Enregistre PasswordResetToken
     * @param passwordResetToken PasswordResetToken
     */
    void save(PasswordResetToken passwordResetToken);

    /**
     * Efface PasswordResetToken de la BD
     * @param passwordResetToken PasswordResetToken
     */
    void delete(PasswordResetToken passwordResetToken);

    /**
     * Efface le token si l'utilisateur demande à nouveau
     * @param userId id user
     */
    void deleteIfExist(Long userId);
}
