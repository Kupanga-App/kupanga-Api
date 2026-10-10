package com.kupanga.api.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MissingRequestCookieException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;

import java.time.LocalDateTime;

/**
 * Filet de sécurité de l'API REST (EXC, A7/A8) : toute exception non métier devient une réponse
 * {@link ApiErrorResponse} au bon statut, avec un message générique. Jamais de stacktrace ni de
 * message interne (classe, SQL, chemin MinIO…) dans la réponse : le détail part uniquement dans les logs.
 * <p>
 * Passe après {@link GlobalExceptionHandler} (exceptions métier et validation) et ne s'applique
 * qu'aux {@code @RestController} : le back-office Thymeleaf garde ses pages d'erreur.
 */
@RestControllerAdvice(annotations = RestController.class)
@Order(Ordered.LOWEST_PRECEDENCE)
public class TechnicalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(TechnicalExceptionHandler.class);

    /** Rôle insuffisant ({@code @PreAuthorize}) : 403. */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, "Accès refusé", request);
    }

    /** Authentification absente ou invalide détectée dans un contrôleur : 401. */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiErrorResponse> handleAuthentication(AuthenticationException ex, HttpServletRequest request) {
        return build(HttpStatus.UNAUTHORIZED, "Authentification requise", request);
    }

    /** {@code /auth/refresh} sans cookie de session : 401 et non 500 (A8). */
    @ExceptionHandler(MissingRequestCookieException.class)
    public ResponseEntity<ApiErrorResponse> handleMissingCookie(MissingRequestCookieException ex, HttpServletRequest request) {
        return build(HttpStatus.UNAUTHORIZED, "Session expirée, veuillez vous reconnecter", request);
    }

    /** Fichier trop volumineux : 413. */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiErrorResponse> handleMaxUploadSize(MaxUploadSizeExceededException ex, HttpServletRequest request) {
        return build(HttpStatus.PAYLOAD_TOO_LARGE, "Fichier trop volumineux", request);
    }

    /**
     * Requête multipart illisible : 400 ; fichier ou requête au-delà des limites : 413 (B3).
     * Avec l'analyse différée ({@code resolve-lazily}), le dépassement de taille levé par Tomcat
     * arrive enveloppé dans une {@link MultipartException} générique : on le reconnaît dans les causes.
     */
    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<ApiErrorResponse> handleMultipart(MultipartException ex, HttpServletRequest request) {
        if (depassementDeTaille(ex)) {
            return build(HttpStatus.PAYLOAD_TOO_LARGE, "Fichier trop volumineux", request);
        }
        return build(HttpStatus.BAD_REQUEST, "Requête multipart invalide", request);
    }

    /** Tomcat : {@code FileSizeLimitExceededException} / {@code SizeLimitExceededException} (sans dépendre de ses classes). */
    private static boolean depassementDeTaille(Throwable ex) {
        for (Throwable cause = ex; cause != null; cause = cause.getCause()) {
            if (cause instanceof MaxUploadSizeExceededException
                    || cause.getClass().getSimpleName().endsWith("SizeLimitExceededException")) {
                return true;
            }
            if (cause.getCause() == cause) break;
        }
        return false;
    }

    /** Paramètre de chemin ou de requête du mauvais type (ex. {@code /biens/abc}) : 400. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "Paramètre invalide : " + ex.getName(), request);
    }

    /** Argument refusé par le code (valeur d'enum inconnue…) : 400, sans le message interne. */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        logger.warn("Argument invalide sur {} : {}", request.getRequestURI(), ex.getMessage());
        return build(HttpStatus.BAD_REQUEST, "Requête invalide", request);
    }

    /**
     * Tout le reste. Les exceptions Spring MVC qui portent leur statut ({@link ErrorResponse} :
     * 415, paramètre manquant…) le gardent ; les autres (dont {@link IllegalStateException},
     * qui signale un bug ou un état interne et non un conflit métier) deviennent une 500 générique.
     * Les règles métier lèvent une {@code BusinessException} avec leur propre statut.
     * <p>
     * Une route inconnue (404) ou une méthode non supportée (405) n'atteint aucun contrôleur :
     * ces cas passent par {@code /error} de Spring Boot (statut correct, sans détail interne).
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleException(Exception ex, HttpServletRequest request) {
        if (ex instanceof ErrorResponse errorResponse) {
            HttpStatusCode code = errorResponse.getStatusCode();
            HttpStatus status = HttpStatus.resolve(code.value());
            if (status != null && status.is4xxClientError()) {
                logger.debug("Erreur client {} sur {} : {}", code.value(), request.getRequestURI(), ex.getMessage());
                return build(status, status.getReasonPhrase(), request);
            }
        }
        logger.error("Erreur inattendue sur {} {}", request.getMethod(), request.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Une erreur interne est survenue", request);
    }

    private ResponseEntity<ApiErrorResponse> build(HttpStatus status, String message, HttpServletRequest request) {
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
