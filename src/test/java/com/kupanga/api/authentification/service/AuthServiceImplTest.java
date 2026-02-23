package com.kupanga.api.authentification.service;

import com.kupanga.api.email.service.EmailService;
import com.kupanga.api.exception.business.KupangaBusinessException;
import com.kupanga.api.authentification.dto.AuthResponseDTO;
import com.kupanga.api.authentification.dto.LoginDTO;
import com.kupanga.api.authentification.entity.PasswordResetToken;
import com.kupanga.api.authentification.service.impl.AuthServiceImpl;
import com.kupanga.api.authentification.utils.JwtUtils;
import com.kupanga.api.minio.service.MinioService;
import com.kupanga.api.user.dto.formDTO.UserFormDTO;
import com.kupanga.api.user.dto.readDTO.UserDTO;
import com.kupanga.api.user.entity.Role;
import com.kupanga.api.user.entity.User;
import com.kupanga.api.user.mapper.UserMapper;
import com.kupanga.api.user.service.UserService;
import com.kupanga.api.authentification.entity.RefreshToken;
import jakarta.servlet.http.HttpServletResponse;
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
}
