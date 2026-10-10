package com.kupanga.api.backoffice.config;

import org.springframework.security.core.AuthenticationException;

/**
 * Limite de tentatives atteinte sur le login du back-office (BO-LOGIN).
 * Hérite d'{@link AuthenticationException} pour que le formLogin redirige vers la page de login
 * au lieu de renvoyer une erreur 500.
 */
public class TropDeTentativesBackOfficeException extends AuthenticationException {

    public TropDeTentativesBackOfficeException() {
        super("Trop de tentatives de connexion");
    }
}
