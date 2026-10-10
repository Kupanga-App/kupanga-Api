package com.kupanga.api.authentification.controller;

import com.kupanga.api.authentification.ratelimit.LimiteTentatives;
import com.kupanga.api.authentification.ratelimit.LimiteurTentatives;
import com.kupanga.api.exception.business.TropDeTentativesException;
import java.util.List;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kupanga.api.exception.business.KupangaBusinessException;
import com.kupanga.api.exception.business.UserNotFoundException;
import com.kupanga.api.authentification.dto.AuthResponseDTO;
import com.kupanga.api.authentification.dto.CompleteGoogleProfileDTO;
import com.kupanga.api.authentification.dto.ForgotPasswordDTO;
import com.kupanga.api.authentification.dto.GoogleLoginDTO;
import com.kupanga.api.authentification.dto.LoginDTO;
import com.kupanga.api.authentification.dto.ResetPasswordDTO;
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
import com.kupanga.api.authentification.service.VerificationEmailService;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static com.kupanga.api.authentification.constant.AuthConstant.*;
import static com.kupanga.api.authentification.constant.AuthConstant.TOKEN_REINITIALISATION_INVALIDE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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
    private VerificationEmailService verificationEmailService;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtUtils jwtUtils;

    @MockBean
    private UserDetailsServiceImpl userDetailsService;

    @MockBean
    private LimiteurTentatives limiteurTentatives;

    // Filet de sécurité indispensable si une config globale traîne
    @MockBean
    private EntityManagerFactory entityManagerFactory;

    private final ObjectMapper objectMapper = new ObjectMapper();


    @Test
    @DisplayName("Doit créer un utilisateur avec une image de profil")
    void createUser_withImage_shouldReturn200() throws Exception {

        UserFormDTO userFormDTO = UserFormDTO.builder()
                .firstName("Jean")
                .lastName("Dupont")
                .mail("test@mail.com")
                .password("Password123")
                .role(Role.ROLE_LOCATAIRE)
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

        when(authService.createAndCompleteUserProfil(any(), any()))
                .thenReturn(COMPTE_CREE_VERIFIER_EMAIL);

        // A14 : 201 + message, plus de jeton (connexion après confirmation de l'e-mail)
        mockMvc.perform(multipart("/auth/register")
                        .file(userFormPart)
                        .file(imagePart))
                .andExpect(status().isCreated())
                .andExpect(content().string(COMPTE_CREE_VERIFIER_EMAIL));
    }

    @Test
    @DisplayName("POST /auth/register — prénom avec balises HTML ou mot de passe > 72 caractères : 400 (VALID)")
    void createUser_nomHtmlOuMotDePasseTropLong_shouldReturn400() throws Exception {

        UserFormDTO nomHtml = UserFormDTO.builder()
                .firstName("<b>Jean</b>")
                .lastName("Dupont")
                .mail("jean@test.com")
                .password("Password1")
                .role(Role.ROLE_LOCATAIRE)
                .build();
        UserFormDTO motDePasseLong = UserFormDTO.builder()
                .firstName("Jean-Pierre")
                .lastName("N'Goma")
                .mail("jean@test.com")
                .password("Aa1" + "a".repeat(70))
                .role(Role.ROLE_LOCATAIRE)
                .build();

        for (UserFormDTO dto : List.of(nomHtml, motDePasseLong)) {
            MockMultipartFile userFormPart = new MockMultipartFile(
                    "userFormDTO", "", "application/json", objectMapper.writeValueAsBytes(dto));

            mockMvc.perform(multipart("/auth/register").file(userFormPart))
                    .andExpect(status().isBadRequest());
        }

        verify(authService, never()).createAndCompleteUserProfil(any(), any());
    }

    @Test
    @DisplayName("POST /auth/register — mot de passe faible / e-mail invalide : 400, aucun compte créé (A1)")
    void createUser_invalidForm_shouldReturn400() throws Exception {

        UserFormDTO userFormDTO = UserFormDTO.builder()
                .firstName("Jean")
                .lastName("Dupont")
                .mail("pas-un-email")
                .password("faible")
                .role(Role.ROLE_LOCATAIRE)
                .build();

        MockMultipartFile userFormPart = new MockMultipartFile(
                "userFormDTO", "", "application/json", objectMapper.writeValueAsBytes(userFormDTO));

        mockMvc.perform(multipart("/auth/register").file(userFormPart))
                .andExpect(status().isBadRequest());

        verify(authService, never()).createAndCompleteUserProfil(any(), any());
    }

    @Test
    @DisplayName("Doit créer un utilisateur sans image de profil")
    void createUser_withoutImage_shouldReturn200() throws Exception {

        UserFormDTO userFormDTO = UserFormDTO.builder()
                .firstName("Jean")
                .lastName("Dupont")
                .mail("test@mail.com")
                .password("Password123")
                .role(Role.ROLE_LOCATAIRE)
                .build();

        MockMultipartFile userFormPart = new MockMultipartFile(
                "userFormDTO",
                "",
                "application/json",
                objectMapper.writeValueAsBytes(userFormDTO)
        );

        when(authService.createAndCompleteUserProfil(any(), isNull()))
                .thenReturn(COMPTE_CREE_VERIFIER_EMAIL);

        mockMvc.perform(multipart("/auth/register")
                        .file(userFormPart))
                .andExpect(status().isCreated())
                .andExpect(content().string(COMPTE_CREE_VERIFIER_EMAIL));
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
    // LIMITE DE TENTATIVES (A3)
    // =============================
    @Test
    @DisplayName("POST /auth/login — limite atteinte : 429 + Retry-After, aucune vérification du mot de passe (A3)")
    void login_limiteAtteinte_shouldReturn429() throws Exception {
        doThrow(new TropDeTentativesException(600))
                .when(limiteurTentatives).verifierEmail(LimiteTentatives.LOGIN_PAR_EMAIL, "alice@test.com");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"alice@test.com\", \"password\": \"Abcd1234\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "600"));

        verify(authService, never()).login(any(), any());
    }

    @Test
    @DisplayName("Routes limitées : login (IP, e-mail+IP, e-mail), forgot-password (IP, e-mail), register (IP, e-mail), google (IP) (A3)")
    void routesAuth_appellentLeLimiteur() throws Exception {
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\": \"alice@test.com\", \"password\": \"Abcd1234\"}"));
        mockMvc.perform(post("/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\": \"alice@test.com\"}"));
        mockMvc.perform(post("/auth/google")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"idToken\": \"token-google\"}"));
        mockMvc.perform(multipart("/auth/register").file(new MockMultipartFile(
                "userFormDTO", "", "application/json", objectMapper.writeValueAsBytes(UserFormDTO.builder()
                        .firstName("Alice").lastName("Martin").mail("alice@test.com")
                        .password("Password1").role(Role.ROLE_LOCATAIRE).build()))));

        verify(limiteurTentatives).verifierIp(eq(LimiteTentatives.LOGIN_PAR_IP), any());
        verify(limiteurTentatives).verifierEmailEtIp(eq(LimiteTentatives.LOGIN_PAR_EMAIL_ET_IP), eq("alice@test.com"), any());
        verify(limiteurTentatives).verifierEmail(LimiteTentatives.LOGIN_PAR_EMAIL, "alice@test.com");
        verify(limiteurTentatives).verifierIp(eq(LimiteTentatives.FORGOT_PASSWORD_PAR_IP), any());
        verify(limiteurTentatives).verifierEmail(LimiteTentatives.FORGOT_PASSWORD_PAR_EMAIL, "alice@test.com");
        verify(limiteurTentatives).verifierIp(eq(LimiteTentatives.GOOGLE_PAR_IP), any());
        verify(limiteurTentatives).verifierIp(eq(LimiteTentatives.REGISTER_PAR_IP), any());
        verify(limiteurTentatives).verifierEmail(LimiteTentatives.REGISTER_PAR_EMAIL, "alice@test.com");
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
    @DisplayName("POST /auth/refresh sans cookie : 401 et non 500 (A8)")
    void refresh_sansCookie_shouldReturn401() throws Exception {
        mockMvc.perform(post("/auth/refresh"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Session expirée, veuillez vous reconnecter"));

        verify(authService, never()).refresh(anyString());
    }

    @Test
    @DisplayName("POST /auth/refresh depuis un site inconnu (cookie envoyé) : 403, aucun token émis (A11)")
    void refresh_origineInconnue_shouldReturn403() throws Exception {
        mockMvc.perform(post("/auth/refresh")
                        .header("Origin", "https://site-pirate.example")
                        .cookie(new Cookie("refreshToken", "refresh-token")))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));

        verify(authService, never()).refresh(anyString());
    }

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
    @DisplayName("POST /forgot-password — e-mail dans le body JSON, message générique (P0-2)")
    void forgotPassword_shouldReturnGenericMessage() throws Exception {

        String email = "test@kupanga.com";

        when(authService.forgotPassword(email))
                .thenReturn(MAIL_REINITIALISATION_ENVOYE);

        mockMvc.perform(post("/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ForgotPasswordDTO(email))))
                .andExpect(status().isOk())
                .andExpect(content().string(MAIL_REINITIALISATION_ENVOYE));

        verify(authService).forgotPassword(email);
    }

    @Test
    @DisplayName("POST /forgot-password — e-mail en query string refusé (P0-2)")
    void forgotPassword_shouldRejectQueryParam() throws Exception {

        mockMvc.perform(post("/auth/forgot-password")
                        .param("email", "test@kupanga.com"))
                .andExpect(status().is4xxClientError());

        verify(authService, never()).forgotPassword(any());
    }

    @Test
    @DisplayName("POST /forgot-password — e-mail mal formé : 400")
    void forgotPassword_shouldReturnBadRequest_whenEmailInvalid() throws Exception {

        mockMvc.perform(post("/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ForgotPasswordDTO("pas-un-email"))))
                .andExpect(status().isBadRequest());

        verify(authService, never()).forgotPassword(any());
    }

    @Test
    @DisplayName("POST /reset-password — succès : token et mot de passe dans le body JSON")
    void resetPassword_shouldReturnOk() throws Exception {

        String token = "valid-token";
        String newPassword = "NewPassword@123";

        when(authService.resetPassword(token, newPassword))
                .thenReturn("Mot de passe mis à jour");

        mockMvc.perform(post("/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ResetPasswordDTO(token, newPassword))))
                .andExpect(status().isOk())
                .andExpect(content().string("Mot de passe mis à jour"));

        verify(authService).resetPassword(token, newPassword);
    }

    @Test
    @DisplayName("POST /reset-password — token et mot de passe en query string refusés (P0-2)")
    void resetPassword_shouldRejectQueryParams() throws Exception {

        mockMvc.perform(post("/auth/reset-password")
                        .param("token", "valid-token")
                        .param("newPassword", "NewPassword@123"))
                .andExpect(status().is4xxClientError());

        verify(authService, never()).resetPassword(any(), any());
    }

    @Test
    @DisplayName("POST /reset-password — mot de passe trop faible : 400 (P0-2)")
    void resetPassword_shouldReturnBadRequest_whenPasswordWeak() throws Exception {

        mockMvc.perform(post("/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ResetPasswordDTO("valid-token", "faible"))))
                .andExpect(status().isBadRequest());

        verify(authService, never()).resetPassword(any(), any());
    }

    @Test
    @DisplayName("POST /reset-password — erreur : token expiré ou invalide")
    void resetPassword_shouldReturnBadRequest_whenTokenIsInvalid() throws Exception {

        String token = "expired-token";
        String newPassword = "NewPassword@123";

        when(authService.resetPassword(token, newPassword))
                .thenThrow(new KupangaBusinessException(TOKEN_REINITIALISATION_INVALIDE, HttpStatus.BAD_REQUEST));

        mockMvc.perform(post("/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ResetPasswordDTO(token, newPassword))))
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
                .andExpect(jsonPath("$.lastName").value("Doe"))
                .andExpect(jsonPath("$.password").doesNotExist());
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


    // ─── A14 : confirmation de l'adresse e-mail ─────────────────────────────

    @Test
    @DisplayName("POST /auth/verifier-email — route publique, jeton dans le corps : 200")
    void verifierEmail_ok() throws Exception {
        when(verificationEmailService.verifier("jeton-123")).thenReturn(EMAIL_VERIFIE);

        mockMvc.perform(post("/auth/verifier-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\": \"jeton-123\"}"))
                .andExpect(status().isOk())
                .andExpect(content().string(EMAIL_VERIFIE));
    }

    @Test
    @DisplayName("POST /auth/verifier-email — jeton vide ou trop long : 400, aucune vérification")
    void verifierEmail_jetonInvalide_400() throws Exception {
        for (String corps : new String[] {"{}", "{\"token\": \"\"}", "{\"token\": \"" + "a".repeat(65) + "\"}"}) {
            mockMvc.perform(post("/auth/verifier-email").contentType(MediaType.APPLICATION_JSON).content(corps))
                    .andExpect(status().isBadRequest());
        }
        verifyNoInteractions(verificationEmailService);
    }

    @Test
    @DisplayName("POST /auth/renvoyer-verification — route publique limitée (A3) par IP puis par e-mail : 200 générique")
    void renvoyerVerification_limiteEtReponseGenerique() throws Exception {
        when(verificationEmailService.renvoyer("alice@test.com")).thenReturn(LIEN_VERIFICATION_ENVOYE);

        mockMvc.perform(post("/auth/renvoyer-verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"alice@test.com\"}"))
                .andExpect(status().isOk())
                .andExpect(content().string(LIEN_VERIFICATION_ENVOYE));

        verify(limiteurTentatives).verifierIp(eq(LimiteTentatives.RENVOI_VERIFICATION_PAR_IP), any());
        verify(limiteurTentatives).verifierEmail(LimiteTentatives.RENVOI_VERIFICATION_PAR_EMAIL, "alice@test.com");
    }
}
