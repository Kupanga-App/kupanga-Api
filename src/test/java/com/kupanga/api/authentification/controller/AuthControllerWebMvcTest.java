package com.kupanga.api.authentification.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kupanga.api.exception.business.KupangaBusinessException;
import com.kupanga.api.exception.business.TokenExpiredException;
import com.kupanga.api.exception.business.UserNotFoundException;
import com.kupanga.api.authentification.dto.AuthResponseDTO;
import com.kupanga.api.authentification.dto.CompleteGoogleProfileDTO;
import com.kupanga.api.authentification.dto.GoogleLoginDTO;
import com.kupanga.api.authentification.dto.LoginDTO;
import com.kupanga.api.authentification.service.AuthService;
import com.kupanga.api.authentification.service.impl.UserDetailsServiceImpl;
import com.kupanga.api.authentification.utils.JwtUtils;
import com.kupanga.api.user.dto.formDTO.UserFormDTO;
import com.kupanga.api.user.dto.readDTO.UserDTO;
import com.kupanga.api.user.entity.Role;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import com.kupanga.api.config.SecurityConfig;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.springframework.security.test.context.support.WithMockUser;
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
@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = true)
@DisplayName("Tests pour LoginController via MockMvc")
class AuthControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtUtils jwtUtils;

    @MockBean
    private UserDetailsServiceImpl userDetailsService;

    // Filet de sécurité indispensable si une config globale traîne
    @MockBean
    private EntityManagerFactory entityManagerFactory;

    private final ObjectMapper objectMapper = new ObjectMapper();


    @Test
    @DisplayName("Doit créer un utilisateur avec une image de profil")
    void createUser_withImage_shouldReturn200() throws Exception {

        UserFormDTO userFormDTO = UserFormDTO.builder()
                .mail("test@mail.com")
                .password("password123")
                .build();

        MockMultipartFile userFormPart = new MockMultipartFile(
                "userFormDTO",
                "",
                "application/json",
                objectMapper.writeValueAsBytes(userFormDTO)
        );

        MockMultipartFile imagePart = new MockMultipartFile(
                "imageProfil",
                "photo.png",
                "image/png",
                "fake-image-content".getBytes()
        );

        AuthResponseDTO responseDTO = AuthResponseDTO.builder()
                .accessToken("token123")
                .requiresRoleSelection(false)
                .build();

        when(authService.createAndCompleteUserProfil(any(), any(), any()))
                .thenReturn(responseDTO);

        mockMvc.perform(multipart("/auth/register")
                        .file(userFormPart)
                        .file(imagePart))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("token123"));
    }

    @Test
    @DisplayName("Doit créer un utilisateur sans image de profil")
    void createUser_withoutImage_shouldReturn200() throws Exception {

        UserFormDTO userFormDTO = UserFormDTO.builder()
                .mail("test@mail.com")
                .password("password123")
                .build();

        MockMultipartFile userFormPart = new MockMultipartFile(
                "userFormDTO",
                "",
                "application/json",
                objectMapper.writeValueAsBytes(userFormDTO)
        );

        AuthResponseDTO responseDTO = AuthResponseDTO.builder()
                .accessToken("token123")
                .requiresRoleSelection(false)
                .build();

        when(authService.createAndCompleteUserProfil(any(), isNull(), any()))
                .thenReturn(responseDTO);

        mockMvc.perform(multipart("/auth/register")
                        .file(userFormPart))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("token123"));
    }

    @Test
    @DisplayName("Doit retourner 400 si le userFormDTO est absent")
    void createUser_withoutUserFormDTO_shouldReturn400() throws Exception {

        MockMultipartFile imagePart = new MockMultipartFile(
                "imageProfil",
                "photo.png",
                "image/png",
                "fake-image-content".getBytes()
        );

        mockMvc.perform(multipart("/auth/register")
                        .file(imagePart))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Doit retourner 400 si le JSON du userFormDTO est invalide")
    void createUser_withInvalidJson_shouldReturn400() throws Exception {

        MockMultipartFile invalidUserForm = new MockMultipartFile(
                "userFormDTO",
                "",
                "application/json",
                "{invalid-json}".getBytes()
        );

        mockMvc.perform(multipart("/auth/register")
                        .file(invalidUserForm))
                .andExpect(status().isBadRequest());
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

        // Mock du service login
        when(authService.login(any(LoginDTO.class), any()))
                .thenReturn(authResponseDTO);

        // DTO envoyé à l'API
        LoginDTO loginDTO = LoginDTO.builder()
                .email("user.mechant@gmail.com")
                .password("Abcd1234")
                .build();

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

        when(authService.refresh(refreshToken)).thenReturn(newToken);

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

        when(authService.logout(any(), any())).thenReturn("Déconnexion réussie");

        mockMvc.perform(post("/auth/logout")
                        .cookie(new Cookie("refreshToken", refreshToken)))
                .andExpect(status().isOk())
                .andExpect(content().string("Déconnexion réussie"));
    }

    @Test
    @DisplayName("POST /auth/logout : succès déconnexion sans token")
    void testLogoutWithoutToken() throws Exception {
        when(authService.logout(any(), any())).thenReturn("Déconnexion réussie");

        mockMvc.perform(post("/auth/logout"))
                .andExpect(status().isOk())
                .andExpect(content().string("Déconnexion réussie"));
    }

    @Test
    @DisplayName("POST /forgot-password — succès : email de réinitialisation envoyé")
    void forgotPassword_shouldReturnOk() throws Exception {

        String email = "test@kupanga.com";

        when(authService.forgotPassword(email))
                .thenReturn("Email de réinitialisation envoyé");

        mockMvc.perform(post("/auth/forgot-password")
                        .param("email", email))
                .andExpect(status().isOk())
                .andExpect(content().string("Email de réinitialisation envoyé"));

        verify(authService).forgotPassword(email);
    }

    @Test
    @DisplayName("POST /forgot-password — erreur : email inexistant")
    void forgotPassword_shouldReturnNotFound_whenEmailDoesNotExist() throws Exception {

        String email = "invalide@kupanga.com";

        when(authService.forgotPassword(email))
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

        when(authService.resetPassword(token, newPassword))
                .thenReturn("Mot de passe mis à jour");

        mockMvc.perform(post("/auth/reset-password")
                        .param("token", token)
                        .param("newPassword", newPassword))
                .andExpect(status().isOk())
                .andExpect(content().string("Mot de passe mis à jour"));

        verify(authService).resetPassword(token, newPassword);
    }

    @Test
    @DisplayName("POST /reset-password — erreur : token expiré ou invalide")
    void resetPassword_shouldReturnBadRequest_whenTokenIsInvalid() throws Exception {

        String token = "expired-token";
        String newPassword = "NewPassword@123";

        when(authService.resetPassword(token, newPassword))
                .thenThrow(new TokenExpiredException());

        mockMvc.perform(post("/auth/reset-password")
                        .param("token", token)
                        .param("newPassword", newPassword))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Doit retourner les informations de l'utilisateur connecté")
    @WithMockUser(username = "test@mail.com")
    void me_authenticatedUser_shouldReturnUserInfos() throws Exception {

        UserDTO userDTO = UserDTO.builder()
                .mail("test@mail.com")
                .firstName("John")
                .lastName("Doe")
                .build();

        when(authService.getUserInfos("test@mail.com"))
                .thenReturn(userDTO);

        mockMvc.perform(get("/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mail").value("test@mail.com"))
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.lastName").value("Doe"));
    }

    @Test
    @DisplayName("Doit retourner 404 si l'utilisateur n'existe pas")
    @WithMockUser(username = "unknown@mail.com")
    void me_userNotFound_shouldReturn404() throws Exception {

        when(authService.getUserInfos("unknown@mail.com"))
                .thenThrow(new UserNotFoundException("Utilisateur introuvable"));

        mockMvc.perform(get("/auth/me"))
                .andExpect(status().isNotFound());
    }

    // =============================
    // TESTS GOOGLE OAUTH2
    // =============================

    @Test
    @DisplayName("POST /auth/google : utilisateur existant — connexion normale, requiresRoleSelection=false")
    void loginWithGoogle_existingUser_shouldReturnToken() throws Exception {
        GoogleLoginDTO dto = new GoogleLoginDTO("valid-google-id-token");

        AuthResponseDTO responseDTO = AuthResponseDTO.builder()
                .accessToken("google-access-token")
                .requiresRoleSelection(false)
                .build();

        when(authService.loginWithGoogle(any(GoogleLoginDTO.class), any()))
                .thenReturn(responseDTO);

        mockMvc.perform(post("/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("google-access-token"))
                .andExpect(jsonPath("$.requiresRoleSelection").value(false));
    }

    @Test
    @DisplayName("POST /auth/google : nouvel utilisateur — requiresRoleSelection=true")
    void loginWithGoogle_newUser_shouldRequireRoleSelection() throws Exception {
        GoogleLoginDTO dto = new GoogleLoginDTO("new-user-google-token");

        AuthResponseDTO responseDTO = AuthResponseDTO.builder()
                .accessToken("google-access-token")
                .requiresRoleSelection(true)
                .build();

        when(authService.loginWithGoogle(any(GoogleLoginDTO.class), any()))
                .thenReturn(responseDTO);

        mockMvc.perform(post("/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requiresRoleSelection").value(true));
    }

    @Test
    @DisplayName("POST /auth/google : token Google invalide — 401")
    void loginWithGoogle_invalidToken_shouldReturn401() throws Exception {
        GoogleLoginDTO dto = new GoogleLoginDTO("invalid-google-token");

        when(authService.loginWithGoogle(any(GoogleLoginDTO.class), any()))
                .thenThrow(new KupangaBusinessException("Token Google invalide ou expiré", HttpStatus.UNAUTHORIZED));

        mockMvc.perform(post("/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /auth/google : idToken absent — 400")
    void loginWithGoogle_missingToken_shouldReturn400() throws Exception {
        String body = "{\"idToken\": \"\"}";

        mockMvc.perform(post("/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PATCH /auth/complete-profile : succès — rôle assigné, nouveau JWT retourné")
    @WithMockUser(username = "google-user@gmail.com")
    void completeProfile_success_shouldReturnToken() throws Exception {
        CompleteGoogleProfileDTO dto = new CompleteGoogleProfileDTO(Role.ROLE_LOCATAIRE);

        AuthResponseDTO responseDTO = AuthResponseDTO.builder()
                .accessToken("completed-access-token")
                .requiresRoleSelection(false)
                .build();

        when(authService.completeGoogleProfile(any(CompleteGoogleProfileDTO.class), any(), any()))
                .thenReturn(responseDTO);

        mockMvc.perform(patch("/auth/complete-profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("completed-access-token"))
                .andExpect(jsonPath("$.requiresRoleSelection").value(false));
    }

    @Test
    @DisplayName("PATCH /auth/complete-profile : profil déjà complété — 400")
    @WithMockUser(username = "google-user@gmail.com")
    void completeProfile_alreadyCompleted_shouldReturn400() throws Exception {
        CompleteGoogleProfileDTO dto = new CompleteGoogleProfileDTO(Role.ROLE_LOCATAIRE);

        when(authService.completeGoogleProfile(any(CompleteGoogleProfileDTO.class), any(), any()))
                .thenThrow(new KupangaBusinessException("Le profil est déjà complété", HttpStatus.BAD_REQUEST));

        mockMvc.perform(patch("/auth/complete-profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

}