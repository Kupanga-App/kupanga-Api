package com.kupanga.api.authentification.service;

import com.kupanga.api.authentification.dto.AuthResponseDTO;
import com.kupanga.api.authentification.dto.CompleteGoogleProfileDTO;
import com.kupanga.api.authentification.dto.GoogleLoginDTO;
import com.kupanga.api.authentification.dto.LoginDTO;
import com.kupanga.api.authentification.entity.PasswordResetToken;
import com.kupanga.api.authentification.entity.RefreshToken;
import com.kupanga.api.authentification.google.GoogleTokenVerifier;
import com.kupanga.api.authentification.google.GoogleUserInfo;
import com.kupanga.api.authentification.service.impl.AuthServiceImpl;
import com.kupanga.api.authentification.utils.JwtUtils;
import com.kupanga.api.email.service.EmailService;
import com.kupanga.api.exception.business.KupangaBusinessException;
import com.kupanga.api.minio.service.MinioService;
import com.kupanga.api.user.dto.formDTO.UserFormDTO;
import com.kupanga.api.user.dto.readDTO.UserDTO;
import com.kupanga.api.user.entity.Role;
import com.kupanga.api.user.entity.User;
import com.kupanga.api.user.mapper.UserMapper;
import com.kupanga.api.user.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.http.HttpHeaders;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.time.LocalDateTime;

import static com.kupanga.api.authentification.constant.AuthConstant.MOT_DE_PASSE_A_JOUR;
import static com.kupanga.api.minio.constant.MinioConstant.PHOTO_PROFIL_BUCKET;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("Tests unitaires pour LoginServiceImpl")
class AuthServiceImplTest {

    @Mock
    private UserService userService;

    @Mock
    private EmailService emailService;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private PasswordResetTokenService passwordResetTokenService ;

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private HttpServletResponse response;

    @Mock
    private MinioService minioService;

    @Mock
    private GoogleTokenVerifier googleTokenVerifier;

    @InjectMocks
    private AuthServiceImpl loginService;

    private User utilisateur;
    private LoginDTO loginDTO ;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        utilisateur = User.builder()
                .mail("user@example.com")
                .password("encodedPassword")
                .build();
        loginDTO = new LoginDTO("test@example.com" ,"encodedPassword" );
    }

    // ====================== Tests login ======================

    @Test
    @DisplayName("login() : connexion réussie, cookie refresh ajouté, access token retourné")
    void testLoginSuccess() {
        LoginDTO loginDTO = new LoginDTO("user@example.com", "password");

        when(userService.getUserByEmail(loginDTO.email())).thenReturn(utilisateur);
        doNothing().when(userService).isCorrectPassword(loginDTO.password(), utilisateur.getPassword());
        when(jwtUtils.generateAccessToken(utilisateur.getMail(), String.valueOf(utilisateur.getRole())))
                .thenReturn("accessToken");
        when(refreshTokenService.createRefreshToken(utilisateur)).thenReturn("refreshToken");

        AuthResponseDTO result = loginService.login(loginDTO, response);

        assertThat(result.accessToken()).isEqualTo("accessToken");

        ArgumentCaptor<String> headerCaptor = ArgumentCaptor.forClass(String.class);
        verify(response).addHeader(eq(HttpHeaders.SET_COOKIE), headerCaptor.capture());
        assertThat(headerCaptor.getValue()).contains("refreshToken=refreshToken");
    }

    @Test
    @DisplayName("login() : mot de passe incorrect lance exception")
    void testLoginIncorrectPassword() {
        LoginDTO loginDTO = new LoginDTO("user@example.com", "wrongpassword");
        when(userService.getUserByEmail(loginDTO.email())).thenReturn(utilisateur);
        doThrow(new RuntimeException("Mot de passe incorrect"))
                .when(userService).isCorrectPassword(loginDTO.password(), utilisateur.getPassword());

        assertThrows(RuntimeException.class, () -> loginService.login(loginDTO, response));
        verify(response, never()).addHeader(any(), any());
    }

    // ====================== Tests refresh ======================

    @Test
    @DisplayName("refresh() : token valide génère nouvel access token")
    void testRefreshSuccess() {
        RefreshToken refreshToken = RefreshToken.builder()
                .token("validToken")
                .user(utilisateur)
                .expiration(Instant.now().plusSeconds(3600))
                .revoked(false)
                .build();

        when(refreshTokenService.getByToken("validToken")).thenReturn(refreshToken);
        when(jwtUtils.generateAccessToken(utilisateur.getMail(), utilisateur.getPassword()))
                .thenReturn("newAccessToken");

        AuthResponseDTO result = loginService.refresh("validToken");

        assertThat(result.accessToken()).isEqualTo("newAccessToken");
    }

    @Test
    @DisplayName("refresh() : token expiré ou révoqué lance KupangaBusinessException")
    void testRefreshExpiredOrRevokedToken() {
        RefreshToken revokedToken = RefreshToken.builder()
                .token("revokedToken")
                .user(utilisateur)
                .expiration(Instant.now().minusSeconds(10))
                .revoked(true)
                .build();

        when(refreshTokenService.getByToken("revokedToken")).thenReturn(revokedToken);

        assertThrows(KupangaBusinessException.class,
                () -> loginService.refresh("revokedToken"));
    }

    // ====================== Tests logout ======================

    @Test
    @DisplayName("logout() : token fourni révoqué et cookie supprimé")
    void testLogoutWithToken() {
        String token = "refreshToken";

        String result = loginService.logout(token, response);

        assertThat(result).contains("Réussie");
        verify(refreshTokenService).deleteRefreshToken(token);

        ArgumentCaptor<String> headerCaptor = ArgumentCaptor.forClass(String.class);
        verify(response).addHeader(eq(HttpHeaders.SET_COOKIE), headerCaptor.capture());
        String cookieHeader = headerCaptor.getValue();

        // Vérifie que le cookie commence par le nom et contient Max-Age=0
        assertThat(cookieHeader).startsWith("refreshToken=");
        assertThat(cookieHeader).contains("Max-Age=0");
    }


    @Test
    @DisplayName("logout() : pas de token fourni continue normalement")
    void testLogoutWithoutToken() {
        String result = loginService.logout(null, response);

        assertThat(result).contains("Réussie");
        verify(refreshTokenService, never()).deleteRefreshToken(any());
        verify(response, never()).addHeader(any(), any());
    }


    // ======================
    // Tests forgotPassword
    // ======================

    @Test
    @DisplayName("forgotPassword — retourne un token valide et envoie un mail")
    void forgotPassword_shouldReturnToken_andSendMail() {
        when(userService.getUserByEmail("user@kupanga.com")).thenReturn(utilisateur);

        String token = loginService.forgotPassword("user@kupanga.com");

        assertNotNull(token);
        verify(passwordResetTokenService, times(1)).save(any(PasswordResetToken.class));
        verify(emailService, times(1)).sendPasswordResetMail(eq("user@kupanga.com"), contains(token));
    }

    @Test
    @DisplayName("forgotPassword — lance une exception si l'email n'existe pas")
    void forgotPassword_shouldThrowException_whenEmailDoesNotExist() {
        when(userService.getUserByEmail("invalide@kupanga.com"))
                .thenThrow(new RuntimeException("Utilisateur introuvable"));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> loginService.forgotPassword("invalide@kupanga.com"));

        assertEquals("Utilisateur introuvable", exception.getMessage());
        verify(passwordResetTokenService, never()).save(any());
        verify(emailService, never()).sendPasswordResetMail(any(), any());
    }

    // ======================
    // Tests resetPassword
    // ======================

    @Test
    @DisplayName("resetPassword — met à jour le mot de passe et envoie confirmation")
    void resetPassword_shouldUpdatePassword_andSendConfirmation() {
        PasswordResetToken token = PasswordResetToken.builder()
                .token("123")
                .user(utilisateur)
                .expirationDate(LocalDateTime.now().plusMinutes(10))
                .build();

        when(passwordResetTokenService.getByToken("123")).thenReturn(token);
        when(passwordEncoder.encode("newPassword")).thenReturn("encodedPassword");

        String result = loginService.resetPassword("123", "newPassword");

        assertEquals(MOT_DE_PASSE_A_JOUR, result);
        assertEquals("encodedPassword", utilisateur.getPassword());
        verify(userService, times(1)).save(utilisateur);
        verify(passwordResetTokenService, times(1)).delete(token);
        verify(emailService, times(1)).sendPasswordUpdatedConfirmation(utilisateur.getMail());
    }

    @Test
    @DisplayName("resetPassword — lance une exception si le token est expiré")
    void resetPassword_shouldThrowException_whenTokenExpired() {
        PasswordResetToken token = PasswordResetToken.builder()
                .token("123")
                .user(utilisateur)
                .expirationDate(LocalDateTime.now().minusMinutes(1))
                .build();

        when(passwordResetTokenService.getByToken("123")).thenReturn(token);

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> loginService.resetPassword("123", "newPassword"));

        assertEquals("Token expiré", exception.getMessage());
        verify(userService, never()).save(any());
        verify(passwordResetTokenService, never()).delete(any());
        verify(emailService, never()).sendPasswordUpdatedConfirmation(any());
    }

    @Test
    @DisplayName("createAndCompleteUserProfil() : succès avec image Minio")
    void testCreateAndCompleteUserProfil_withImage() {

        UserFormDTO form = new UserFormDTO(
                "User",
                "password",
                "john@mail.com",
                "John",
                Role.ROLE_LOCATAIRE,
                "defaultUrl"
        );

        MultipartFile image = mock(MultipartFile.class);
        when(image.isEmpty()).thenReturn(false);

        when(passwordEncoder.encode("password")).thenReturn("encodedPwd");
        when(minioService.uploadImage(image, PHOTO_PROFIL_BUCKET))
                .thenReturn("minioUrl");

        // capturer l'utilisateur sauvegardé
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        doNothing().when(userService).save(userCaptor.capture());

        // login() va chercher l'utilisateur -> on renvoie celui sauvegardé
        when(userService.getUserByEmail(any()))
                .thenAnswer(inv -> userCaptor.getValue());

        doNothing().when(userService)
                .isCorrectPassword(any(), any());

        when(jwtUtils.generateAccessToken(any(), any()))
                .thenReturn("accessToken");

        when(refreshTokenService.createRefreshToken(any()))
                .thenReturn("refreshToken");

        AuthResponseDTO result =
                loginService.createAndCompleteUserProfil(form, image, response);

        assertThat(result.accessToken()).isEqualTo("accessToken");

        verify(minioService).uploadImage(image, PHOTO_PROFIL_BUCKET);
        verify(userService).save(any(User.class));
        verify(emailService).sendWelcomeMessage("john@mail.com","User");
    }

    @Test
    @DisplayName("getUserInfos() : retourne le DTO utilisateur")
    void testGetUserInfos() {

        String email = "user@mail.com";

        User user = new User();
        UserDTO dto = UserDTO.builder().build();

        when(userService.getUserByEmail(email)).thenReturn(user);
        when(userMapper.toDTO(user)).thenReturn(dto);

        UserDTO result = loginService.getUserInfos(email);

        assertThat(result).isEqualTo(dto);

        verify(userService).getUserByEmail(email);
        verify(userMapper).toDTO(user);
    }

    // ══════════════════════════════════════════════════════════════
    // loginWithGoogle
    // ══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("loginWithGoogle() — utilisateur existant par googleId → connexion sans sélection rôle")
    void loginWithGoogle_existingUserByGoogleId_returnsNoRoleSelection() {
        GoogleLoginDTO dto = new GoogleLoginDTO("google-id-token");
        GoogleUserInfo googleInfo = new GoogleUserInfo("g-123", "user@example.com", "Jean", "Dupont", null);

        User existingUser = User.builder()
                .mail("user@example.com")
                .googleId("g-123")
                .role(Role.ROLE_LOCATAIRE)
                .build();

        when(googleTokenVerifier.verify("google-id-token")).thenReturn(googleInfo);
        when(userService.findOptionalByGoogleId("g-123")).thenReturn(Optional.of(existingUser));
        when(jwtUtils.generateAccessToken(existingUser.getMail(), String.valueOf(existingUser.getRole())))
                .thenReturn("accessToken");
        when(refreshTokenService.createRefreshToken(existingUser)).thenReturn("refreshToken");

        AuthResponseDTO result = loginService.loginWithGoogle(dto, response);

        assertThat(result.accessToken()).isEqualTo("accessToken");
        assertThat(result.requiresRoleSelection()).isFalse();
    }

    @Test
    @DisplayName("loginWithGoogle() — nouvel utilisateur → compte créé, requiresRoleSelection=true")
    void loginWithGoogle_newUser_createsAccountAndRequiresRoleSelection() {
        GoogleLoginDTO dto = new GoogleLoginDTO("google-id-token");
        GoogleUserInfo googleInfo = new GoogleUserInfo("g-999", "new@example.com", "Marie", "Curie", null);

        User newUser = User.builder()
                .mail("new@example.com")
                .googleId("g-999")
                .build(); // role = null

        when(googleTokenVerifier.verify("google-id-token")).thenReturn(googleInfo);
        when(userService.findOptionalByGoogleId("g-999")).thenReturn(Optional.empty());
        when(userService.findOptionalByMail("new@example.com")).thenReturn(Optional.empty());
        doNothing().when(userService).save(any(User.class));
        when(jwtUtils.generateAccessToken("new@example.com", "")).thenReturn("pendingToken");
        when(refreshTokenService.createRefreshToken(any(User.class))).thenReturn("refreshToken");

        AuthResponseDTO result = loginService.loginWithGoogle(dto, response);

        assertThat(result.requiresRoleSelection()).isTrue();
        verify(userService).save(any(User.class));
    }

    @Test
    @DisplayName("loginWithGoogle() — email existant (compte classique) → Google ID lié, connexion normale")
    void loginWithGoogle_existingEmailAccount_linksGoogleId() {
        GoogleLoginDTO dto = new GoogleLoginDTO("google-id-token");
        GoogleUserInfo googleInfo = new GoogleUserInfo("g-456", "existing@example.com", "Paul", "Martin", null);

        User existingUser = User.builder()
                .mail("existing@example.com")
                .role(Role.ROLE_PROPRIETAIRE)
                .build(); // googleId = null

        when(googleTokenVerifier.verify("google-id-token")).thenReturn(googleInfo);
        when(userService.findOptionalByGoogleId("g-456")).thenReturn(Optional.empty());
        when(userService.findOptionalByMail("existing@example.com")).thenReturn(Optional.of(existingUser));
        doNothing().when(userService).save(existingUser);
        when(jwtUtils.generateAccessToken(existingUser.getMail(), String.valueOf(existingUser.getRole())))
                .thenReturn("accessToken");
        when(refreshTokenService.createRefreshToken(existingUser)).thenReturn("refreshToken");

        AuthResponseDTO result = loginService.loginWithGoogle(dto, response);

        assertThat(result.requiresRoleSelection()).isFalse();
        assertThat(existingUser.getGoogleId()).isEqualTo("g-456");
        verify(userService).save(existingUser);
    }

    @Test
    @DisplayName("loginWithGoogle() — token Google invalide → KupangaBusinessException 401")
    void loginWithGoogle_invalidToken_throwsException() {
        GoogleLoginDTO dto = new GoogleLoginDTO("invalid-token");

        when(googleTokenVerifier.verify("invalid-token"))
                .thenThrow(new KupangaBusinessException("Token Google invalide ou expiré", org.springframework.http.HttpStatus.UNAUTHORIZED));

        assertThrows(KupangaBusinessException.class, () -> loginService.loginWithGoogle(dto, response));
        verifyNoInteractions(userService, refreshTokenService);
    }

    // ══════════════════════════════════════════════════════════════
    // completeGoogleProfile
    // ══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("completeGoogleProfile() — succès : rôle assigné, nouveau JWT retourné")
    void completeGoogleProfile_success_assignsRoleAndReturnsToken() {
        CompleteGoogleProfileDTO dto = new CompleteGoogleProfileDTO(Role.ROLE_LOCATAIRE);

        User user = User.builder()
                .mail("new@example.com")
                .googleId("g-999")
                .build(); // role = null

        when(userService.getUserByEmail("new@example.com")).thenReturn(user);
        doNothing().when(userService).verifyIfRoleOfUserValid(Role.ROLE_LOCATAIRE);
        doNothing().when(userService).save(user);
        when(jwtUtils.generateAccessToken("new@example.com", String.valueOf(Role.ROLE_LOCATAIRE)))
                .thenReturn("finalToken");
        when(refreshTokenService.createRefreshToken(user)).thenReturn("refreshToken");

        AuthResponseDTO result = loginService.completeGoogleProfile(dto, "new@example.com", response);

        assertThat(result.accessToken()).isEqualTo("finalToken");
        assertThat(result.requiresRoleSelection()).isFalse();
        assertThat(user.getRole()).isEqualTo(Role.ROLE_LOCATAIRE);
        assertThat(user.getHasCompleteProfil()).isTrue();
        verify(userService).save(user);
    }

    @Test
    @DisplayName("completeGoogleProfile() — utilisateur non Google → KupangaBusinessException 400")
    void completeGoogleProfile_nonGoogleUser_throwsBadRequest() {
        CompleteGoogleProfileDTO dto = new CompleteGoogleProfileDTO(Role.ROLE_LOCATAIRE);

        User user = User.builder()
                .mail("classic@example.com")
                .password("encodedPwd")
                .role(Role.ROLE_PROPRIETAIRE)
                .build(); // googleId = null

        when(userService.getUserByEmail("classic@example.com")).thenReturn(user);

        KupangaBusinessException ex = assertThrows(KupangaBusinessException.class,
                () -> loginService.completeGoogleProfile(dto, "classic@example.com", response));

        assertThat(ex.getStatus()).isEqualTo(org.springframework.http.HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("completeGoogleProfile() — profil déjà complété → KupangaBusinessException 400")
    void completeGoogleProfile_alreadyCompleted_throwsBadRequest() {
        CompleteGoogleProfileDTO dto = new CompleteGoogleProfileDTO(Role.ROLE_LOCATAIRE);

        User user = User.builder()
                .mail("new@example.com")
                .googleId("g-999")
                .role(Role.ROLE_LOCATAIRE) // déjà un rôle
                .build();

        when(userService.getUserByEmail("new@example.com")).thenReturn(user);

        KupangaBusinessException ex = assertThrows(KupangaBusinessException.class,
                () -> loginService.completeGoogleProfile(dto, "new@example.com", response));

        assertThat(ex.getStatus()).isEqualTo(org.springframework.http.HttpStatus.BAD_REQUEST);
        verify(userService, never()).save(any());
    }
}
