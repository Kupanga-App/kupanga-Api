package com.kupanga.api.authentification.service;

import com.kupanga.api.exception.business.KupangaBusinessException;
import com.kupanga.api.authentification.entity.RefreshToken;
import com.kupanga.api.authentification.repository.RefreshTokenRepository;
import com.kupanga.api.authentification.service.impl.RefreshTokenServiceImpl;
import com.kupanga.api.utilisateur.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@DisplayName("Tests unitaires pour RefreshTokenServiceImpl")
class RefreshTokenServiceImplTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private RefreshTokenServiceImpl refreshTokenService;

    @Captor
    private ArgumentCaptor<RefreshToken> refreshTokenCaptor;

    private User user;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        user = new User();
        user.setId(1L);
        user.setMail("user@test.com");
    }

    @Test
    @DisplayName("createRefreshToken : crée un nouveau token si aucun existant")
    void shouldCreateNewRefreshTokenWhenNoneExists() {
        when(refreshTokenRepository.findByUserId(user.getId())).thenReturn(null);

        String token = refreshTokenService.createRefreshToken(user);

        assertThat(token).isNotNull();
        verify(refreshTokenRepository).save(refreshTokenCaptor.capture());
        RefreshToken savedToken = refreshTokenCaptor.getValue();
        assertThat(savedToken.getToken()).isEqualTo(token);
        assertThat(savedToken.getUser()).isEqualTo(user);
        assertThat(savedToken.getRevoked()).isFalse();
        assertThat(savedToken.getExpiration()).isAfter(Instant.now());
    }

    @Test
    @DisplayName("createRefreshToken : supprime l'ancien token avant d'en créer un nouveau")
    void shouldDeleteExistingRefreshTokenBeforeCreatingNew() {
        RefreshToken oldToken = new RefreshToken();
        oldToken.setToken(UUID.randomUUID().toString());
        oldToken.setUser(user);

        when(refreshTokenRepository.findByUserId(user.getId())).thenReturn(oldToken);

        String newToken = refreshTokenService.createRefreshToken(user);

        // Vérifie que l'ancien token a été supprimé
        verify(refreshTokenRepository).delete(oldToken);

        // Vérifie que le nouveau token a été sauvegardé
        verify(refreshTokenRepository).save(refreshTokenCaptor.capture());
        RefreshToken savedToken = refreshTokenCaptor.getValue();
        assertThat(savedToken.getToken()).isEqualTo(newToken);
    }

    @Test
    @DisplayName("getByToken : retourne le token existant")
    void shouldReturnRefreshTokenIfExists() {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken("existingToken");

        when(refreshTokenRepository.findByToken("existingToken"))
                .thenReturn(Optional.of(refreshToken));

        RefreshToken result = refreshTokenService.getByToken("existingToken");

        assertThat(result).isEqualTo(refreshToken);
    }

    @Test
    @DisplayName("getByToken : lance exception si token inexistant")
    void shouldThrowExceptionIfTokenNotFound() {
        when(refreshTokenRepository.findByToken("nonexistent"))
                .thenReturn(Optional.empty());

        assertThrows(KupangaBusinessException.class,
                () -> refreshTokenService.getByToken("nonexistent"));
    }

    @Test
    @DisplayName("deleteRefreshToken : supprime le token existant")
    void shouldDeleteRefreshTokenIfExists() {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken("tokenToDelete");

        when(refreshTokenRepository.findByToken("tokenToDelete"))
                .thenReturn(Optional.of(refreshToken));

        refreshTokenService.deleteRefreshToken("tokenToDelete");

        verify(refreshTokenRepository).delete(refreshToken);
    }

    @Test
    @DisplayName("deleteRefreshToken : lance exception si token inexistant")
    void shouldThrowExceptionWhenDeletingNonExistentToken() {
        when(refreshTokenRepository.findByToken("nonexistent"))
                .thenReturn(Optional.empty());

        assertThrows(KupangaBusinessException.class,
                () -> refreshTokenService.deleteRefreshToken("nonexistent"));

        verify(refreshTokenRepository, never()).delete(any());
    }
}
