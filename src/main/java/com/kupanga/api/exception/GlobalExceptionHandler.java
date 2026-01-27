package com.kupanga.api.exception;

import com.kupanga.api.exception.business.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Gestionnaire global des exceptions pour l'application.
 *
 * <p>
 * Toutes les exceptions de type {@link BusinessException} sont interceptées ici
 * et renvoient une réponse HTTP structurée contenant :
 * </p>
 * <ul>
 *     <li>status : code HTTP</li>
 *     <li>error : libellé standard du code HTTP</li>
 *     <li>Message : message métier détaillé</li>
 *     <li>path : URI de la requête ayant généré l'erreur</li>
 *     <li>Timestamp : date et heure de l'erreur</li>
 * </ul>
 */
@RestControllerAdvice
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
