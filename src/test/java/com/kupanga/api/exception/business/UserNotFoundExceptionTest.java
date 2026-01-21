package com.kupanga.api.exception.business;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Tests unitaires pour UserNotFoundException")
class UserNotFoundExceptionTest {

    @Test
    @DisplayName(" Doit construire l'exception avec le message correct et HttpStatus.NOT_FOUND")
    void testConstructor() {
        String email = "inexistant@example.com";
        UserNotFoundException ex = new UserNotFoundException(email);

        // Vérifie le message de l'exception
        assertThat(ex.getMessage()).isEqualTo("Aucun utilisateur trouvé pour l'email : " + email);

        // Vérifie le statut HTTP associé
        assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);

        // Vérifie que l'exception est bien une sous-classe de BusinessException
        assertThat(ex).isInstanceOf(BusinessException.class)
                .isInstanceOf(RuntimeException.class);
    }
}
