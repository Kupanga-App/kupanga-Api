package com.kupanga.api.exception.business;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Tests unitaires pour InvalidRoleException")
class InvalidRoleExceptionTest {

    @Test
    @DisplayName(" Constructeur par défaut doit avoir le message et le statut corrects")
    void testDefaultConstructor() {
        InvalidRoleException ex = new InvalidRoleException();

        assertThat(ex.getMessage()).isEqualTo("Rôle métier invalide ");
        assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(ex).isInstanceOf(BusinessException.class)
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName(" Constructeur avec message personnalisé doit conserver le message et le statut")
    void testConstructorWithCustomMessage() {
        String customMessage = "Rôle inconnu pour cet utilisateur";
        InvalidRoleException ex = new InvalidRoleException(customMessage);

        assertThat(ex.getMessage()).isEqualTo(customMessage);
        assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(ex).isInstanceOf(BusinessException.class)
                .isInstanceOf(RuntimeException.class);
    }
}
