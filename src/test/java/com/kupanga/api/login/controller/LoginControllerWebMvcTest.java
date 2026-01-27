package com.kupanga.api.login.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kupanga.api.exception.business.TokenExpiredException;
import com.kupanga.api.exception.business.UserAlreadyExistsException;
import com.kupanga.api.exception.business.UserNotFoundException;
import com.kupanga.api.login.dto.AuthResponseDTO;
import com.kupanga.api.login.dto.LoginDTO;
import com.kupanga.api.login.service.LoginService;
import com.kupanga.api.login.service.impl.UserDetailsServiceImpl;
import com.kupanga.api.login.utils.JwtUtils;
import com.kupanga.api.utilisateur.dto.formDTO.UserFormDTO;
import com.kupanga.api.utilisateur.dto.readDTO.UserDTO;
import com.kupanga.api.utilisateur.entity.Role;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import com.kupanga.api.config.SecurityConfig;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Correction complète pour éviter l'erreur 'BeanCreationException: ...
 * jpaSharedEM_entityManagerFactory'.
 *
 * Stratégie :
 * 1. spring.jpa.open-in-view=false : Empêche OpenEntityManagerInViewInterceptor
 * de demander un EntityManager.
 * 2. Exclusions : Bloque l'auto-config de JPA et des Repositories.
 * 3. @MockBean EntityManagerFactory : Satisfait toute dépendance résiduelle.
 */
@WebMvcTest(controllers = LoginController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
@DisplayName("Tests pour LoginController via MockMvc")
class LoginControllerWebMvcTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private LoginService loginService;

        @MockBean
        private JwtUtils jwtUtils;

        @MockBean
        private UserDetailsServiceImpl userDetailsService;

        // Filet de sécurité indispensable si une config globale traîne
        @MockBean
        private EntityManagerFactory entityManagerFactory;

        private ObjectMapper objectMapper = new ObjectMapper();

        private UserFormDTO userFormDTO;
        private LoginDTO loginDTO;

        @BeforeEach
        void setUp() {
                userFormDTO = UserFormDTO.builder()
                                .email("user@example.com")
                                .role(Role.ROLE_LOCATAIRE)
                                .build();

                loginDTO = new LoginDTO("user@example.com", "password");
        }

        // =============================
        // TEST CREATION UTILISATEUR
        // =============================
        @Test
        @DisplayName("POST /auth/create-count : succès création utilisateur")
        void testCreateUserSuccess() throws Exception {
                UserDTO userDTO = UserDTO.builder()
                                .id(1L)
                                .mail("user@example.com")
                                .role(Role.ROLE_LOCATAIRE)
                                .build();

                when(loginService.creationUtilisateur(any()))
                                .thenReturn(userDTO);

                mockMvc.perform(post("/auth/create-count")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(userFormDTO)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.mail").value("user@example.com"))
                                .andExpect(jsonPath("$.role").value("ROLE_LOCATAIRE"));
        }

        @Test
        @DisplayName("POST /auth/create-count : utilisateur déjà existant")
        void testCreateUserAlreadyExists() throws Exception {
                when(loginService.creationUtilisateur(any()))
                                .thenThrow(new UserAlreadyExistsException(loginDTO.email()));

                mockMvc.perform(post("/auth/create-count")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(userFormDTO)))
                                .andExpect(status().isConflict());
        }

        // =============================
        // TEST LOGIN
        // =============================
        @Test
        @DisplayName("POST /auth/login : connexion réussie")
        void testLoginSuccess() throws Exception {
                AuthResponseDTO authResponseDTO = AuthResponseDTO.builder()
                                .accessToken("access-token")
                                .build();

                when(loginService.login(any(LoginDTO.class), any()))
                                .thenReturn(authResponseDTO);

                mockMvc.perform(post("/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(loginDTO)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.accessToken").value("access-token"));
        }

        // =============================
        // TEST REFRESH TOKEN
        // =============================
        @Test
        @DisplayName("POST /auth/refresh : succès refresh token")
        void testRefreshTokenSuccess() throws Exception {
                String refreshToken = "refresh-token";
                AuthResponseDTO newToken = AuthResponseDTO.builder()
                                .accessToken("new-access-token")
                                .build();

                when(loginService.refresh(refreshToken)).thenReturn(newToken);

                mockMvc.perform(post("/auth/refresh")
                                .cookie(new Cookie("refreshToken", refreshToken)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.accessToken").value("new-access-token"));
        }

        // =============================
        // TEST LOGOUT
        // =============================
        @Test
        @DisplayName("POST /auth/logout : succès déconnexion avec token")
        void testLogoutWithToken() throws Exception {
                String refreshToken = "refresh-token";

                when(loginService.logout(any(), any())).thenReturn("Déconnexion réussie");

                mockMvc.perform(post("/auth/logout")
                                .cookie(new Cookie("refreshToken", refreshToken)))
                                .andExpect(status().isOk())
                                .andExpect(content().string("Déconnexion réussie"));
        }

        @Test
        @DisplayName("POST /auth/logout : succès déconnexion sans token")
        void testLogoutWithoutToken() throws Exception {
                when(loginService.logout(any(), any())).thenReturn("Déconnexion réussie");

                mockMvc.perform(post("/auth/logout"))
                                .andExpect(status().isOk())
                                .andExpect(content().string("Déconnexion réussie"));
        }

    @Test
    @DisplayName("POST /forgot-password — succès : email de réinitialisation envoyé")
    void forgotPassword_shouldReturnOk() throws Exception {

        String email = "test@kupanga.com";

        when(loginService.forgotPassword(email))
                .thenReturn("Email de réinitialisation envoyé");

        mockMvc.perform(post("/auth/forgot-password")
                        .param("email", email))
                .andExpect(status().isOk())
                .andExpect(content().string("Email de réinitialisation envoyé"));

        verify(loginService).forgotPassword(email);
    }

    @Test
    @DisplayName("POST /forgot-password — erreur : email inexistant")
    void forgotPassword_shouldReturnNotFound_whenEmailDoesNotExist() throws Exception {

        String email = "invalide@kupanga.com";

        when(loginService.forgotPassword(email))
                .thenThrow(new UserNotFoundException(email));

        mockMvc.perform(post("/auth/forgot-password")
                        .param("email", email))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message")
                        .value("Aucun utilisateur trouvé pour l'email : " + email));
    }

    @Test
    @DisplayName("POST /reset-password — succès : mot de passe mis à jour")
    void resetPassword_shouldReturnOk() throws Exception {

        String token = "valid-token";
        String newPassword = "NewPassword@123";

        when(loginService.resetPassword(token, newPassword))
                .thenReturn("Mot de passe mis à jour");

        mockMvc.perform(post("/auth/reset-password")
                        .param("token", token)
                        .param("newPassword", newPassword))
                .andExpect(status().isOk())
                .andExpect(content().string("Mot de passe mis à jour"));

        verify(loginService).resetPassword(token, newPassword);
    }

    @Test
    @DisplayName("POST /reset-password — erreur : token expiré ou invalide")
    void resetPassword_shouldReturnBadRequest_whenTokenIsInvalid() throws Exception {

        String token = "expired-token";
        String newPassword = "NewPassword@123";

        when(loginService.resetPassword(token, newPassword))
                .thenThrow(new TokenExpiredException());

        mockMvc.perform(post("/auth/reset-password")
                        .param("token", token)
                        .param("newPassword", newPassword))
                .andExpect(status().isBadRequest());
    }




}