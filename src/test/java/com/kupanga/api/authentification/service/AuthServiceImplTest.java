package com.kupanga.api.authentification.service;

import static com.kupanga.api.authentification.constant.AuthConstant.*;
import com.kupanga.api.authentification.service.VerificationEmailService;
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
import com.kupanga.api.exception.business.InvalidPasswordException;
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
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.time.LocalDateTime;

import static com.kupanga.api.authentification.constant.AuthConstant.MAIL_REINITIALISATION_ENVOYE;
import static com.kupanga.api.authentification.constant.AuthConstant.MOT_DE_PASSE_A_JOUR;
import static com.kupanga.api.authentification.constant.AuthConstant.TOKEN_REINITIALISATION_INVALIDE;
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

    @Mock
    private VerificationEmailService verificationEmailService;

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
                .emailVerifie(true)
                .build();
        loginDTO = new LoginDTO("test@example.com" ,"encodedPassword" );
    }

    // ====================== Tests login ======================

    @Test
    @DisplayName("login() : bon mot de passe mais e-mail non confirmé → 403, aucun jeton (A14)")
    void testLogin_emailNonVerifie_refuse() {
        utilisateur.setEmailVerifie(false);
        when(userService.findOptionalByMail("user@example.com")).thenReturn(Optional.of(utilisateur));
        doNothing().when(userService).isCorrectPassword(any(), any());

        KupangaBusinessException ex = assertThrows(KupangaBusinessException.class,
                () -> loginService.login(new LoginDTO("user@example.com", "password"), response));

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(ex.getMessage()).isEqualTo(EMAIL_NON_VERIFIE);
        verifyNoInteractions(jwtUtils, refreshTokenService);
    }

    @Test
    @DisplayName("login() : mauvais mot de passe sur un compte non confirmé → 401 générique (pas d'indice sur le compte)")
    void testLogin_emailNonVerifie_mauvaisMotDePasse_401() {
        utilisateur.setEmailVerifie(false);
        when(userService.findOptionalByMail("user@example.com")).thenReturn(Optional.of(utilisateur));
        doThrow(new InvalidPasswordException()).when(userService).isCorrectPassword(any(), any());

        assertThrows(InvalidPasswordException.class,
                () -> loginService.login(new LoginDTO("user@example.com", "faux"), response));
    }

    @Test
    @DisplayName("login() : connexion réussie, cookie refresh ajouté, access token retourné")
    void testLoginSuccess() {
        LoginDTO loginDTO = new LoginDTO("user@example.com", "password");

        when(userService.findOptionalByMail(loginDTO.email())).thenReturn(Optional.of(utilisateur));
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
        when(userService.findOptionalByMail(loginDTO.email())).thenReturn(Optional.of(utilisateur));
        doThrow(new InvalidPasswordException())
                .when(userService).isCorrectPassword(loginDTO.password(), utilisateur.getPassword());

        InvalidPasswordException ex = assertThrows(InvalidPasswordException.class,
                () -> loginService.login(loginDTO, response));
        assertThat(ex.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(response, never()).addHeader(any(), any());
    }

    @Test
    @DisplayName("login() : e-mail inconnu → même 401 générique qu'un mauvais mot de passe (A2)")
    void testLoginUnknownEmailSameErrorAsWrongPassword() {
        LoginDTO loginDTO = new LoginDTO("inconnu@example.com", "Password1");
        when(userService.findOptionalByMail(loginDTO.email())).thenReturn(Optional.empty());

        InvalidPasswordException ex = assertThrows(InvalidPasswordException.class,
                () -> loginService.login(loginDTO, response));

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(ex.getMessage()).isEqualTo(new InvalidPasswordException().getMessage())
                .doesNotContain("inconnu@example.com");
        verify(userService, never()).getUserByEmail(any());
        // BCrypt est calculé quand même : pas d'énumération par le temps de réponse
        verify(passwordEncoder).matches(eq("Password1"), any());
        verify(response, never()).addHeader(any(), any());
    }

    // ====================== Tests refresh ======================

    @Test
    @DisplayName("refresh() : token valide génère nouvel access token")
    void testRefreshSuccess() {
        utilisateur.setRole(Role.ROLE_PROPRIETAIRE);
        RefreshToken refreshToken = RefreshToken.builder()
                .token("validToken")
                .user(utilisateur)
                .expiration(Instant.now().plusSeconds(3600))
                .revoked(false)
                .build();

        when(refreshTokenService.getByToken("validToken")).thenReturn(refreshToken);
        when(jwtUtils.generateAccessToken(utilisateur.getMail(), String.valueOf(utilisateur.getRole())))
                .thenReturn("newAccessToken");

        AuthResponseDTO result = loginService.refresh("validToken");

        assertThat(result.accessToken()).isEqualTo("newAccessToken");
    }

    @Test
    @DisplayName("refresh() : le claim role contient le rôle, jamais le hash du mot de passe (P0-3)")
    void testRefreshPutsRoleNotPasswordInToken() {
        utilisateur.setRole(Role.ROLE_PROPRIETAIRE);
        utilisateur.setPassword("$2a$10$hashBcryptDuMotDePasse");
        RefreshToken refreshToken = RefreshToken.builder()
                .token("validToken")
                .user(utilisateur)
                .expiration(Instant.now().plusSeconds(3600))
                .revoked(false)
                .build();

        when(refreshTokenService.getByToken("validToken")).thenReturn(refreshToken);
        when(jwtUtils.generateAccessToken(any(), any())).thenReturn("newAccessToken");

        loginService.refresh("validToken");

        ArgumentCaptor<String> roleCaptor = ArgumentCaptor.forClass(String.class);
        verify(jwtUtils).generateAccessToken(eq(utilisateur.getMail()), roleCaptor.capture());
        assertThat(roleCaptor.getValue())
                .isEqualTo(String.valueOf(Role.ROLE_PROPRIETAIRE))
                .doesNotContain("$2a$");
    }

    @Test
    @DisplayName("refresh() : compte Google sans rôle → claim role vide et sélection du rôle requise")
    void testRefreshUserWithoutRoleRequiresRoleSelection() {
        utilisateur.setRole(null);
        RefreshToken refreshToken = RefreshToken.builder()
                .token("validToken")
                .user(utilisateur)
                .expiration(Instant.now().plusSeconds(3600))
                .revoked(false)
                .build();

        when(refreshTokenService.getByToken("validToken")).thenReturn(refreshToken);
        when(jwtUtils.generateAccessToken(utilisateur.getMail(), "")).thenReturn("pendingToken");

        AuthResponseDTO result = loginService.refresh("validToken");

        assertThat(result.accessToken()).isEqualTo("pendingToken");
        assertThat(result.requiresRoleSelection()).isTrue();
        verify(jwtUtils, never()).generateAccessToken(any(), eq("null"));
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
    @DisplayName("forgotPassword — envoie le token par mail et ne le renvoie jamais (P0-2)")
    void forgotPassword_shouldSendTokenByMail_andNeverReturnIt() {
        when(userService.findOptionalByMail("user@example.com")).thenReturn(Optional.of(utilisateur));

        String result = loginService.forgotPassword("user@example.com");

        ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(passwordResetTokenService, times(1)).save(tokenCaptor.capture());
        String token = tokenCaptor.getValue().getToken();

        assertEquals(MAIL_REINITIALISATION_ENVOYE, result);
        assertThat(result).doesNotContain(token);
        verify(emailService, times(1)).sendPasswordResetMail("user@example.com", token);
    }

    @Test
    @DisplayName("forgotPassword — e-mail inconnu : même message générique, aucun mail (P0-2 / A2)")
    void forgotPassword_shouldReturnSameMessage_whenEmailDoesNotExist() {
        when(userService.findOptionalByMail("invalide@kupanga.com")).thenReturn(Optional.empty());

        String result = loginService.forgotPassword("invalide@kupanga.com");

        assertEquals(MAIL_REINITIALISATION_ENVOYE, result);
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
        assertThat(utilisateur.isEmailVerifie()).isTrue(); // A14 : le lien reçu prouve la possession de l'adresse
        verify(passwordResetTokenService, times(1)).delete(token);
        verify(refreshTokenService, times(1)).revokeAllForUser(utilisateur);
        verify(verificationEmailService).annulerLien(utilisateur.getId());
        verify(emailService, times(1)).sendPasswordUpdatedConfirmation(utilisateur.getMail());
    }

    @Test
    @DisplayName("resetPassword — token inconnu : 400 avec le même message qu'un token expiré")
    void resetPassword_shouldThrowSameError_whenTokenUnknown() {
        when(passwordResetTokenService.getByToken("inconnu"))
                .thenThrow(new KupangaBusinessException(TOKEN_REINITIALISATION_INVALIDE, HttpStatus.BAD_REQUEST));

        KupangaBusinessException exception = assertThrows(KupangaBusinessException.class,
                () -> loginService.resetPassword("inconnu", "NewPassword1"));

        assertEquals(TOKEN_REINITIALISATION_INVALIDE, exception.getMessage());
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        verify(userService, never()).save(any());
        verify(refreshTokenService, never()).revokeAllForUser(any());
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

        KupangaBusinessException exception = assertThrows(KupangaBusinessException.class,
                () -> loginService.resetPassword("123", "newPassword"));

        assertEquals(TOKEN_REINITIALISATION_INVALIDE, exception.getMessage());
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        verify(userService, never()).save(any());
        verify(refreshTokenService, never()).revokeAllForUser(any());
        verify(passwordResetTokenService, never()).delete(any());
        verify(emailService, never()).sendPasswordUpdatedConfirmation(any());
    }

    @Test
    @DisplayName("createAndCompleteUserProfil() : compte non vérifié, lien de confirmation envoyé, aucune connexion ni bienvenue (A14)")
    void testCreateAndCompleteUserProfil_withImage() {

        UserFormDTO form = new UserFormDTO(
                "User",
                "password",
                "john@mail.com",
                "John",
                Role.ROLE_LOCATAIRE,
                "defaultUrl"
        );

        MultipartFile image = imagePng();

        when(minioService.estUrlDuBucket("defaultUrl", PHOTO_PROFIL_BUCKET)).thenReturn(true);
        when(passwordEncoder.encode("password")).thenReturn("encodedPwd");
        when(minioService.uploadImage(image, PHOTO_PROFIL_BUCKET))
                .thenReturn("minioUrl");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        doNothing().when(userService).save(userCaptor.capture());

        String result = loginService.createAndCompleteUserProfil(form, image);

        assertThat(result).isEqualTo(COMPTE_CREE_VERIFIER_EMAIL);
        User cree = userCaptor.getValue();
        assertThat(cree.isEmailVerifie()).isFalse();
        assertThat(cree.getUrlProfile()).isEqualTo("minioUrl");
        verify(verificationEmailService).envoyerLien(cree);
        verifyNoInteractions(jwtUtils, refreshTokenService);
        verify(emailService, never()).sendWelcomeMessage(any(), any());
    }

    @Test
    @DisplayName("createAndCompleteUserProfil() : adresse déjà inscrite (vérifiée ou non) → même réponse, aucun compte ni lien, titulaire prévenu (A14)")
    void testCreateAndCompleteUserProfil_adresseDejaInscrite_memeReponse() {
        for (boolean verifie : new boolean[]{true, false}) {
            reset(userService, emailService, verificationEmailService, minioService, passwordEncoder);
            User existant = User.builder().id(7L).mail("john@mail.com").password("hashDuPremier")
                    .role(Role.ROLE_PROPRIETAIRE).emailVerifie(verifie).build();
            UserFormDTO form = UserFormDTO.builder().firstName("John").lastName("User")
                    .mail("John@Mail.com").password("password").role(Role.ROLE_LOCATAIRE).build();
            MultipartFile image = imagePng();
            when(userService.findOptionalByMail("John@Mail.com")).thenReturn(Optional.of(existant));

            String result = loginService.createAndCompleteUserProfil(form, image);

            assertThat(result).isEqualTo(COMPTE_CREE_VERIFIER_EMAIL);
            verify(passwordEncoder).encode("password"); // même calcul BCrypt qu'une vraie inscription
            verify(emailService).envoyerTentativeInscription("john@mail.com");
            verify(userService, never()).save(any(User.class));
            // pas de nouveau lien : il validerait le mot de passe choisi par le premier inscrit
            verifyNoInteractions(verificationEmailService, jwtUtils, refreshTokenService);
            verify(minioService, never()).uploadImage(any(), any());
            assertThat(existant.getPassword()).isEqualTo("hashDuPremier");
            assertThat(existant.getRole()).isEqualTo(Role.ROLE_PROPRIETAIRE);
        }
    }

    @Test
    @DisplayName("createAndCompleteUserProfil() : avatar hors de notre MinIO → 400, aucun compte créé (revue B5)")
    void testCreateAndCompleteUserProfil_avatarExterne_refuse() {
        String pixelDeSuivi = "https://traqueur.example/pixel.gif";
        UserFormDTO form = new UserFormDTO("User", "password", "john@mail.com", "John",
                Role.ROLE_LOCATAIRE, pixelDeSuivi);
        when(minioService.estUrlDuBucket(pixelDeSuivi, PHOTO_PROFIL_BUCKET)).thenReturn(false);

        KupangaBusinessException ex = assertThrows(KupangaBusinessException.class,
                () -> loginService.createAndCompleteUserProfil(form, null));

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(userService, never()).save(any(User.class));
    }

    /** Fichier reconnu comme PNG par sa signature (B5). */
    private static MultipartFile imagePng() {
        byte[] contenu = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0x0D, 'I', 'H', 'D', 'R'};
        return new org.springframework.mock.web.MockMultipartFile("imageProfil", "photo.png", "image/png", contenu);
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
        ArgumentCaptor<User> cree = ArgumentCaptor.forClass(User.class);
        verify(userService).save(cree.capture());
        assertThat(cree.getValue().isEmailVerifie()).isTrue(); // adresse attestée par Google (A14)
    }

    @Test
    @DisplayName("loginWithGoogle() — email existant (compte classique) → Google ID lié, connexion normale")
    void loginWithGoogle_existingEmailAccount_linksGoogleId() {
        GoogleLoginDTO dto = new GoogleLoginDTO("google-id-token");
        GoogleUserInfo googleInfo = new GoogleUserInfo("g-456", "existing@example.com", "Paul", "Martin", null);

        User existingUser = User.builder()
                .mail("existing@example.com")
                .role(Role.ROLE_PROPRIETAIRE)
                .emailVerifie(true)
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
    @DisplayName("loginWithGoogle() — compte local non vérifié : repris par Google, mot de passe/rôle/profil du premier inscrit effacés, lien et sessions annulés (A14)")
    void loginWithGoogle_compteLocalNonVerifie_reprisSansLesDonneesDuPremierInscrit() {
        GoogleLoginDTO dto = new GoogleLoginDTO("google-id-token");
        GoogleUserInfo googleInfo = new GoogleUserInfo("g-789", "victime@example.com", "Vraie", "Personne", "photoGoogle");

        User squatte = User.builder()
                .id(42L)
                .mail("victime@example.com")
                .password("hashDeLAttaquant")
                .role(Role.ROLE_PROPRIETAIRE)
                .firstName("Faux")
                .lastName("Profil")
                .urlProfile("photoAttaquant")
                .hasCompleteProfil(true)
                .emailVerifie(false)
                .build();

        when(googleTokenVerifier.verify("google-id-token")).thenReturn(googleInfo);
        when(userService.findOptionalByGoogleId("g-789")).thenReturn(Optional.empty());
        when(userService.findOptionalByMail("victime@example.com")).thenReturn(Optional.of(squatte));
        when(jwtUtils.generateAccessToken("victime@example.com", "")).thenReturn("accessToken");
        when(refreshTokenService.createRefreshToken(squatte)).thenReturn("refreshToken");

        AuthResponseDTO result = loginService.loginWithGoogle(dto, response);

        assertThat(result.requiresRoleSelection()).isTrue();
        assertThat(squatte.getPassword()).isNull();
        assertThat(squatte.getRole()).isNull();
        assertThat(squatte.getHasCompleteProfil()).isFalse();
        assertThat(squatte.getFirstName()).isEqualTo("Vraie");
        assertThat(squatte.getLastName()).isEqualTo("Personne");
        assertThat(squatte.getUrlProfile()).isEqualTo("photoGoogle");
        assertThat(squatte.isEmailVerifie()).isTrue();
        assertThat(squatte.getGoogleId()).isEqualTo("g-789");
        verify(verificationEmailService).annulerLien(42L);
        verify(passwordResetTokenService).deleteIfExist(42L);
        verify(refreshTokenService).revokeAllForUser(squatte);
        verify(userService).save(squatte);
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
