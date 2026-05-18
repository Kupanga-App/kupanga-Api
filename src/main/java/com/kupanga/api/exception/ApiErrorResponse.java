package com.kupanga.api.exception;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Structure standard des réponses d'erreur renvoyées par l'API.
 * Utilisée par le {@link com.kupanga.api.exception.GlobalExceptionHandler} pour toutes les erreurs.
 */
public record ApiErrorResponse(
        int status,
        String error,
        String message,
        String path,
        LocalDateTime timestamp,
        Map<String, String> validationErrors
) {}
