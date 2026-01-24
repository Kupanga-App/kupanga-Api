package com.kupanga.api.login.service;

import com.kupanga.api.email.service.EmailService;
import com.kupanga.api.exception.business.InvalidRoleException;
import com.kupanga.api.exception.business.KupangaBusinessException;
import com.kupanga.api.exception.business.UserAlreadyExistsException;
import com.kupanga.api.login.dto.AuthResponseDTO;
import com.kupanga.api.login.dto.LoginDTO;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
    private JwtUtils jwtUtils;

    @Mock
    private HttpServletResponse response;

    @InjectMocks
    private LoginServiceImpl loginService;

    private User utilisateur;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        utilisateur = User.builder()
                .mail("user@example.com")
                .password("encodedPassword")
                .role(Role.ROLE_LOCATAIRE)
                .build();
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

        UserDTO result = loginService.creationUtilisateur(email, role);

        assertThat(result.mail()).isEqualTo(email);
        assertThat(result.role()).isEqualTo(role);
        verify(userService).save(any(User.class));
        verify(emailService).SendPasswordProvisional(eq(email), anyString());
    }

    @Test
    @DisplayName("Doit lancer UserAlreadyExistsException si l'utilisateur existe déjà")
    void testCreationUtilisateurUserAlreadyExists() throws UserAlreadyExistsException {
        String email = "exist@example.com";
        Role role = Role.ROLE_LOCATAIRE;

        doThrow(new UserAlreadyExistsException(email))
                .when(userService).verifyIfUserExistWithEmail(email);

        assertThrows(UserAlreadyExistsException.class, () ->
                loginService.creationUtilisateur(email, role));

        verify(userService, never()).save(any());
        verify(emailService, never()).SendPasswordProvisional(anyString(), anyString());
    }

    @Test
    @DisplayName("Doit lancer InvalidRoleException si rôle incorrect")
    void testCreationUtilisateurInvalidRole() throws UserAlreadyExistsException {
        String email = "test@example.com";

        doNothing().when(userService).verifyIfUserExistWithEmail(email);
        doThrow(new InvalidRoleException()).when(userService).verifyIfRoleOfUserValid(any());

        assertThrows(InvalidRoleException.class, () ->
                loginService.creationUtilisateur(email, any()));

        verify(userService, never()).save(any());
        verify(emailService, never()).SendPasswordProvisional(anyString(), anyString());
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
                .when(emailService).SendPasswordProvisional(anyString(), anyString());

        UserDTO result = loginService.creationUtilisateur(email, Role.valueOf(role));

        assertThat(result).isNotNull();
        verify(userService).save(any(User.class));
        verify(emailService).SendPasswordProvisional(eq(email), anyString());
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
}
