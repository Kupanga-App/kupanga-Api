package com.kupanga.api.exception.business;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Levée quand une limite de tentatives est atteinte (A3) : réponse 429 avec l'en-tête {@code Retry-After}.
 */
@Getter
public class TropDeTentativesException extends BusinessException {

    /** Délai avant de pouvoir réessayer, en secondes. */
    private final long retryAfterSecondes;

    public TropDeTentativesException(long retryAfterSecondes) {
        super("Trop de tentatives. Réessayez dans " + Math.max(1, (retryAfterSecondes + 59) / 60) + " minute(s).",
                HttpStatus.TOO_MANY_REQUESTS);
        this.retryAfterSecondes = retryAfterSecondes;
    }
}
