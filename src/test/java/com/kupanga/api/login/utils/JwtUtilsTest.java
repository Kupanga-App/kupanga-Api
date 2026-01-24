package com.kupanga.api.login.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.User;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Base64;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests unitaires pour JwtUtils")
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
        ReflectionTestUtils.setField(jwtUtils, "accessTokenExpirationTime", 1000L); // 1s pour tests rapides
    }

    @Test
    @DisplayName("Génération d'un access token valide avec l'email et le rôle")
    void shouldGenerateValidAccessTokenWithRole() {
        String email = "test@mail.com";
        String role = "ROLE_USER";

        String token = jwtUtils.generateAccessToken(email, role);

        assertNotNull(token, "Le token ne doit pas être null");
        assertEquals(email, jwtUtils.extractUserEmail(token), "L'email extrait doit correspondre");
    }

    @Test
    @DisplayName("Validation d'un token valide avec UserDetails correct")
    void shouldValidateTokenWithCorrectUserDetails() {
        String email = "user@mail.com";
        String role = "ROLE_USER";
        String token = jwtUtils.generateAccessToken(email, role);

        User userDetails = new User(email, "password", Collections.emptyList());

        assertTrue(jwtUtils.validateToken(token, userDetails), "Le token doit être valide pour l'utilisateur correct");
    }

    @Test
    @DisplayName("Validation d'un token invalide si UserDetails incorrect")
    void shouldInvalidateTokenWithWrongUserDetails() {
        String email = "user@mail.com";
        String role = "ROLE_USER";
        String token = jwtUtils.generateAccessToken(email, role);

        User wrongUser = new User("other@mail.com", "password", Collections.emptyList());

        assertFalse(jwtUtils.validateToken(token, wrongUser), "Le token doit être invalide pour un autre utilisateur");
    }

    @Test
    @DisplayName("Token expiré doit être détecté")
    void shouldDetectExpiredToken() throws InterruptedException {
        ReflectionTestUtils.setField(jwtUtils, "accessTokenExpirationTime", 1L);

        String email = "user@mail.com";
        String role = "ROLE_USER";
        String token = jwtUtils.generateAccessToken(email, role);

        // Pause pour expiration
        Thread.sleep(5);

        User userDetails = new User(email, "password", Collections.emptyList());

        boolean isValid;
        try {
            isValid = jwtUtils.validateToken(token, userDetails);
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            isValid = false; // Token expiré → valide = false
        }

        assertFalse(isValid, "Le token doit être expiré");
    }

    @Test
    @DisplayName("Extraction d'une claim spécifique")
    void shouldExtractClaims() {
        String email = "claim@mail.com";
        String role = "ROLE_ADMIN";
        String token = jwtUtils.generateAccessToken(email, role);

        String extractedEmail = jwtUtils.extractUserEmail(token);
        assertEquals(email, extractedEmail, "L'email extrait doit correspondre");

        // Vérifier que la claim "role" est présente via parsing manuel
        var claims = ReflectionTestUtils.invokeMethod(jwtUtils, "extractAllClaims", token);
        assertEquals(role, ((io.jsonwebtoken.Claims) claims).get("role"), "Le rôle extrait doit correspondre");
    }
}
