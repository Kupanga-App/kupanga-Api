package com.kupanga.api.exception;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.kupanga.api.exception.business.BusinessException;
import com.kupanga.api.exception.business.TropDeTentativesException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Gestionnaire global des exceptions pour l'application.
 * Intercepte les {@link BusinessException} et les erreurs de validation
 * pour renvoyer des réponses HTTP structurées via {@link ApiErrorResponse}.
 * Prioritaire sur {@link TechnicalExceptionHandler}, qui traite tout le reste (EXC).
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Intercepte toutes les exceptions métier {@link BusinessException}.
     *
     * @param ex      l'exception levée
     * @param request la requête HTTP ayant causé l'exception
     * @return {@link ResponseEntity} contenant {@link ApiErrorResponse}
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiErrorResponse> handleBusinessException(
            BusinessException ex,
            HttpServletRequest request
    ) {

        logger.warn("Une exception métier est survenue : {}", ex.getMessage(), ex);
        return buildResponse(
                ex.getStatus(),
                ex.getMessage(),
                request
        );
    }

    /**
     * Document modifié par une autre requête entre sa lecture et son écriture (verrou optimiste, B6) :
     * 409, rien n'a été enregistré.
     *
     * @param ex      l'exception levée
     * @param request la requête HTTP ayant causé l'exception
     * @return {@link ResponseEntity} contenant {@link ApiErrorResponse}
     */
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ApiErrorResponse> handleConflitDeVersion(
            OptimisticLockingFailureException ex,
            HttpServletRequest request
    ) {
        logger.warn("Conflit de version sur {} : {}", request.getRequestURI(), ex.getClass().getSimpleName());
        return buildResponse(
                HttpStatus.CONFLICT,
                "Ce document vient d'être modifié. Rechargez la page puis réessayez.",
                request
        );
    }

    /**
     * Limite de tentatives atteinte (A3) : 429 avec l'en-tête {@code Retry-After} (en secondes).
     *
     * @param ex      l'exception levée
     * @param request la requête HTTP ayant causé l'exception
     * @return {@link ResponseEntity} contenant {@link ApiErrorResponse}
     */
    @ExceptionHandler(TropDeTentativesException.class)
    public ResponseEntity<ApiErrorResponse> handleTropDeTentatives(
            TropDeTentativesException ex,
            HttpServletRequest request
    ) {
        ResponseEntity<ApiErrorResponse> reponse = buildResponse(ex.getStatus(), ex.getMessage(), request);
        return ResponseEntity.status(reponse.getStatusCode())
                .header(HttpHeaders.RETRY_AFTER, String.valueOf(ex.getRetryAfterSecondes()))
                .body(reponse.getBody());
    }

    /** Intercepte toutes les exceptions liées à la validation {@link MethodArgumentNotValidException}
     *
     * @param ex l'exception levée
     * @param request la requête HTTP ayant causé l'exception
     * @return {@link ResponseEntity} contenant {@link ApiErrorResponse}
     */

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {

        Map<String, String> fieldErrors = new HashMap<>();

        ex.getBindingResult()
                .getFieldErrors()
                .forEach(error ->
                        fieldErrors.put(
                                error.getField(),
                                error.getDefaultMessage()
                        )
                );

        ApiErrorResponse response = new ApiErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "Erreur de validation des champs",
                request.getRequestURI(),
                LocalDateTime.now() ,
                fieldErrors
        );


        return ResponseEntity.badRequest().body(response);
    }

    /**
     * Gère les erreurs quand le corps de la requête HTTP est invalide.
     * Si une valeur d'enum est incorrecte, affiche les valeurs possibles.
     *
     * @param ex l'exception levée lors de la lecture du corps de la requête
     * @param request la requête HTTP qui a causé l'erreur
     * @return une réponse HTTP 400 avec le message d'erreur
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleNotReadable(
            HttpMessageNotReadableException ex,
            HttpServletRequest request
    ) {
        String message = "Requête invalide";

        Throwable cause = ex.getMostSpecificCause();
        if (cause instanceof InvalidFormatException invalidFormat
                && invalidFormat.getTargetType().isEnum()) {
            String valeur = String.valueOf(invalidFormat.getValue());
            if (valeur.length() > 50) valeur = valeur.substring(0, 50) + "…";
            message = "Valeur invalide : '" + valeur
                    + "'. Valeurs acceptées : "
                    + Arrays.toString(invalidFormat.getTargetType().getEnumConstants());
        }

        return buildResponse(HttpStatus.BAD_REQUEST, message, request);
    }


    /**
     * Construit une réponse d'erreur standardisée pour l'API.
     *
     * @param status  le statut HTTP à renvoyer
     * @param message le message détaillé de l'erreur
     * @param request la requête HTTP ayant généré l'erreur
     * @return {@link ResponseEntity} contenant {@link ApiErrorResponse}
     */
    private ResponseEntity<ApiErrorResponse> buildResponse(
            HttpStatus status,
            String message,
            HttpServletRequest request
    ) {
        ApiErrorResponse response = new ApiErrorResponse(
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getRequestURI(),
                LocalDateTime.now(),
                null
        );

        return new ResponseEntity<>(response, status);
    }
}
