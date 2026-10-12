package com.kupanga.api.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * EXC : les exceptions techniques donnent le bon statut et un message générique, sans détail interne.
 */
@DisplayName("Tests unitaires — TechnicalExceptionHandler")
class TechnicalExceptionHandlerTest {

    private final TechnicalExceptionHandler handler = new TechnicalExceptionHandler();
    private final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");

    @Test
    @DisplayName("Exception inattendue : 500 générique, aucun message interne (SQL, chemin, classe)")
    void exceptionInattendue_500Generique() {
        ResponseEntity<ApiErrorResponse> reponse = handler.handleException(
                new RuntimeException("ERROR: duplicate key value violates unique constraint \"users_mail_key\""), request);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(reponse.getBody().message()).isEqualTo("Une erreur interne est survenue")
                .doesNotContain("duplicate").doesNotContain("users_mail_key");
        assertThat(reponse.getBody().path()).isEqualTo("/api/test");
    }

    @Test
    @DisplayName("Exceptions Spring MVC à statut (404, 405) : statut conservé, pas de 500")
    void exceptionsSpringAStatut_statutConserve() throws Exception {
        assertThat(handler.handleException(new NoResourceFoundException(HttpMethod.GET, "inconnu"), request).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(handler.handleException(new HttpRequestMethodNotSupportedException("DELETE"), request).getStatusCode())
                .isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
        assertThat(handler.handleException(new HttpMediaTypeNotSupportedException("text/plain"), request).getStatusCode())
                .isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
    }

    @Test
    @DisplayName("IllegalArgumentException → 400 ; IllegalStateException (état interne) → 500 ; sans le message interne")
    void illegalArgumentEtState() {
        ResponseEntity<ApiErrorResponse> argument = handler.handleIllegalArgument(
                new IllegalArgumentException("No enum constant TypeElement.XYZ"), request);
        ResponseEntity<ApiErrorResponse> etat = handler.handleException(
                new IllegalStateException("Session/EntityManager is closed"), request);

        assertThat(argument.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(argument.getBody().message()).doesNotContain("TypeElement");
        assertThat(etat.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(etat.getBody().message()).doesNotContain("EntityManager");
    }

    @Test
    @DisplayName("Rôle insuffisant → 403 ; fichier trop gros → 413")
    void accesRefuseEtUpload() {
        assertThat(handler.handleAccessDenied(new AccessDeniedException("Access Denied"), request).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(handler.handleMaxUploadSize(new MaxUploadSizeExceededException(1024), request).getStatusCode())
                .isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
    }
}
