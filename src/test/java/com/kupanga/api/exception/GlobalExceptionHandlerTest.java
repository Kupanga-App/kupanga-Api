package com.kupanga.api.exception;

import com.kupanga.api.exception.business.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
@DisplayName("Tests unitaires pour GlobalExceptionHandlerException")
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("Handle BusinessException retourne ApiErrorResponse avec status, message et path")
    void handleBusinessException_shouldReturnCorrectApiErrorResponse() {
        // GIVEN
        HttpStatus status = HttpStatus.CONFLICT;
        String message = "Conflit métier";
        BusinessException exception = new BusinessException(message, status) {};
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/test");

        // WHEN
        ResponseEntity<ApiErrorResponse> responseEntity = handler.handleBusinessException(exception, request);

        // THEN
        ApiErrorResponse body = responseEntity.getBody();
        assertThat(body).isNotNull();
        assertThat(body.status()).isEqualTo(status.value());
        assertThat(body.error()).isEqualTo(status.getReasonPhrase());
        assertThat(body.message()).isEqualTo(message);
        assertThat(body.path()).isEqualTo("/api/test");
        assertThat(body.timestamp()).isNotNull();

        assertThat(responseEntity.getStatusCode()).isEqualTo(status);
    }

    @Test
    @DisplayName(" BuildResponse construit ApiErrorResponse correctement")
    void buildResponse_shouldConstructApiErrorResponse() throws Exception {
        // Accès à la méthode privée via réflexion
        var method = GlobalExceptionHandler.class.getDeclaredMethod(
                "buildResponse", HttpStatus.class, String.class, HttpServletRequest.class
        );
        method.setAccessible(true);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/test/path");
        HttpStatus status = HttpStatus.BAD_REQUEST;
        String message = "Erreur";

        ResponseEntity<ApiErrorResponse> responseEntity =
                (ResponseEntity<ApiErrorResponse>) method.invoke(handler, status, message, request);

        ApiErrorResponse body = responseEntity.getBody();
        assertThat(body).isNotNull();
        assertThat(body.status()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(body.error()).isEqualTo(HttpStatus.BAD_REQUEST.getReasonPhrase());
        assertThat(body.message()).isEqualTo(message);
        assertThat(body.path()).isEqualTo("/test/path");
        assertThat(body.timestamp()).isNotNull();
    }
}
