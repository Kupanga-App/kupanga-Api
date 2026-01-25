package com.kupanga.api.login.service;

import com.kupanga.api.login.entity.PasswordResetToken;

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
}
