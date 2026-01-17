package com.kupanga.api.login.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class JwtUtilsTest {

    private JwtUtils jwtUtils;

    @BeforeEach
    void setUp() {
        jwtUtils = new JwtUtils();

        // Clé de test sécurisée (>= 256 bits) encodée en Base64
        String secureTestKey = Base64.getEncoder().encodeToString(
                "this-is-a-very-secure-test-key-32bytes!".getBytes()
        );

        ReflectionTestUtils.setField(jwtUtils, "secretKey", secureTestKey);
        ReflectionTestUtils.setField(jwtUtils, "accessTokenExpirationTime", 1000L);
        ReflectionTestUtils.setField(jwtUtils, "refreshTokenExpirationTime", 2000L);
    }

    @Test
    @DisplayName("Génération d'un access token valide avec l'email comme subject")
    void shouldGenerateValidAccessToken() {
        // WHEN
        String token = jwtUtils.generateAccessToken("test@mail.com");

        // THEN
        assertNotNull(token);
        assertEquals("test@mail.com", jwtUtils.extractUserEmail(token));
    }

    @Test
    @DisplayName("Le refresh token doit être différent de l'access token")
    void shouldGenerateDifferentRefreshToken() {
        // WHEN
        String accessToken = jwtUtils.generateAccessToken("test@mail.com");
        String refreshToken = jwtUtils.generateRefreshToken("test@mail.com");

        // THEN
        assertNotNull(refreshToken);
        assertNotEquals(accessToken, refreshToken);
    }
}



