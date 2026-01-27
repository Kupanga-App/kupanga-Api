package com.kupanga.api.authentification.service;

import com.kupanga.api.authentification.entity.PasswordResetToken;
import com.kupanga.api.authentification.repository.PasswordResetTokenRepository;
import com.kupanga.api.authentification.service.impl.PasswordResetTokenServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests unitaires du service PasswordResetTokenService")
class PasswordResetTokenServiceImplTest {

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @InjectMocks
    private PasswordResetTokenServiceImpl passwordResetTokenService;

    @Test
    @DisplayName("getByToken — retourne le token lorsqu'il existe")
    void getByToken_shouldReturnToken_whenTokenExists() {

        String tokenValue = "valid-token";
        PasswordResetToken token = new PasswordResetToken();
        token.setToken(tokenValue);

        when(passwordResetTokenRepository.findByToken(tokenValue))
                .thenReturn(Optional.of(token));

        PasswordResetToken result =
                passwordResetTokenService.getByToken(tokenValue);

        assertNotNull(result);
        assertEquals(tokenValue, result.getToken());

        verify(passwordResetTokenRepository).findByToken(tokenValue);
    }

    @Test
    @DisplayName("getByToken — lève une exception lorsque le token est invalide")
    void getByToken_shouldThrowException_whenTokenDoesNotExist() {

        String tokenValue = "invalid-token";

        when(passwordResetTokenRepository.findByToken(tokenValue))
                .thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> passwordResetTokenService.getByToken(tokenValue)
        );

        assertEquals("Token invalide", exception.getMessage());

        verify(passwordResetTokenRepository).findByToken(tokenValue);
    }

    @Test
    @DisplayName("save — sauvegarde correctement le token")
    void save_shouldSavePasswordResetToken() {

        PasswordResetToken token = new PasswordResetToken();

        passwordResetTokenService.save(token);

        verify(passwordResetTokenRepository).save(token);
    }


    @Test
    @DisplayName("delete — supprime correctement le token")
    void delete_shouldDeletePasswordResetToken() {

        PasswordResetToken token = new PasswordResetToken();

        passwordResetTokenService.delete(token);

        verify(passwordResetTokenRepository).delete(token);
    }
}
