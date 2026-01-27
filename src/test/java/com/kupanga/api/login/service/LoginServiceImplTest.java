package com.kupanga.api.login.service;

import com.kupanga.api.email.service.EmailService;
import com.kupanga.api.exception.business.KupangaBusinessException;
import com.kupanga.api.exception.business.UserAlreadyExistsException;
import com.kupanga.api.login.dto.AuthResponseDTO;
import com.kupanga.api.login.dto.LoginDTO;
import com.kupanga.api.login.entity.PasswordResetToken;
import com.kupanga.api.login.service.impl.LoginServiceImpl;
import com.kupanga.api.login.utils.JwtUtils;
import com.kupanga.api.utilisateur.dto.readDTO.UserDTO;
import com.kupanga.api.utilisateur.entity.Role;
import com.kupanga.api.utilisateur.entity.User;
import com.kupanga.api.utilisateur.mapper.UserMapper;
import com.kupanga.api.utilisateur.service.UserService;
import com.kupanga.api.login.entity.RefreshToken;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.http.HttpHeaders;

import java.time.Instant;
import java.time.LocalDateTime;

import static com.kupanga.api.login.constant.LoginConstant.MOT_DE_PASSE_A_JOUR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("Tests unitaires pour LoginServiceImpl")
class LoginServiceImplTest {

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

    @InjectMocks
    private LoginServiceImpl loginService;

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

    // ====================== Tests création compte ======================

    @Test
    @DisplayName("Doit créer un utilisateur avec rôle correct et envoyer email")
    void testCreationUtilisateurSuccess() throws UserAlreadyExistsException {
        String email = "test@example.com";
        Role role = Role.ROLE_LOCATAIRE;

        User user = User.builder()
                .mail(email)
                .password("encodedPassword")
                .role(Role.ROLE_LOCATAIRE)
                .build();

        UserDTO userDTO = UserDTO.builder()
                .mail(email)
                .role(role)
                .build();

        doNothing().when(userService).verifyIfUserExistWithEmail(email);
        doNothing().when(userService).verifyIfRoleOfUserValid(role);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userMapper.toDTO(any(User.class))).thenReturn(userDTO);

        UserDTO result = loginService.creationUtilisateur(loginDTO);

        assertThat(result.mail()).isEqualTo(email);
        assertThat(result.role()).isEqualTo(role);
        verify(userService).save(any(User.class));
        verify(emailService).SendWelcomeMessage(eq(email));
    }

    @Test
    @DisplayName("Doit lancer UserAlreadyExistsException si l'utilisateur existe déjà")
    void testCreationUtilisateurUserAlreadyExists() throws UserAlreadyExistsException {
        String email = "test@example.com";

        doThrow(new UserAlreadyExistsException(email))
                .when(userService).verifyIfUserExistWithEmail(email);

        assertThrows(UserAlreadyExistsException.class, () ->
                loginService.creationUtilisateur(loginDTO));

        verify(userService, never()).save(any());
        verify(emailService, never()).SendWelcomeMessage(anyString());
    }

    @Test
    @DisplayName("Doit continuer si l'envoi d'email échoue")
    void testCreationUtilisateurEmailException() throws UserAlreadyExistsException {
        String email = "test@example.com";
        String role = "ROLE_LOCATAIRE";

        doNothing().when(userService).verifyIfUserExistWithEmail(email);
        doNothing().when(userService).verifyIfRoleOfUserValid(Role.valueOf(role));
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userMapper.toDTO(any(User.class))).thenReturn(UserDTO.builder().build());

        doThrow(new RuntimeException("Erreur email"))
                .when(emailService).SendWelcomeMessage(anyString());

        UserDTO result = loginService.creationUtilisateur(loginDTO);

        assertThat(result).isNotNull();
        verify(userService).save(any(User.class));
        verify(emailService).SendWelcomeMessage(eq(email));
    }

    // ====================== Tests login ======================

    @Test
    @DisplayName("login() : connexion réussie, cookie refresh ajouté, access token retourné")
    void testLoginSuccess() {
        LoginDTO loginDTO = new LoginDTO("user@example.com", "password");

        when(userService.getUserByEmail(loginDTO.email())).thenReturn(utilisateur);
        doNothing().when(userService).isCorrectPassword(loginDTO.motDepasse(), utilisateur.getPassword());
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
                .when(userService).isCorrectPassword(loginDTO.motDepasse(), utilisateur.getPassword());

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
}
