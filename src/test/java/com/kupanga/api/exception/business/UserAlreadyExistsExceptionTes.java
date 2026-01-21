package com.kupanga.api.exception.business;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Tests unitaires pour UserAlreadyExistsException")
class UserAlreadyExistsExceptionTest {

    @Test
    @DisplayName(" Doit construire l'exception avec le message correct et HttpStatus.CONFLICT")
    void testConstructor() {
        String email = "test@example.com";
        UserAlreadyExistsException ex = new UserAlreadyExistsException(email);

        // Vérification du message
        assertThat(ex.getMessage()).isEqualTo("Un utilisateur existe déjà avec l'email : " + email);

        // Vérification du statut HTTP
        assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT);

        // Vérification des types de l'exception
        assertThat(ex).isInstanceOf(BusinessException.class)
                .isInstanceOf(RuntimeException.class);
    }
}
