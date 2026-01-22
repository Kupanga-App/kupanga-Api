package com.kupanga.api.utilisateur.service;

import com.kupanga.api.exception.business.InvalidRoleException;
import com.kupanga.api.exception.business.UserAlreadyExistsException;
import com.kupanga.api.exception.business.UserNotFoundException;
import com.kupanga.api.login.dto.AuthResponseDTO;
import com.kupanga.api.login.dto.LoginDTO;
import com.kupanga.api.login.utils.JwtUtils;
import com.kupanga.api.utilisateur.entity.Role;
import com.kupanga.api.utilisateur.entity.Utilisateur;
import com.kupanga.api.utilisateur.repository.UtilisateurRepository;
import com.kupanga.api.utilisateur.service.impl.UtilisateurServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests pour le service utilisateur")
class UtilisateurServiceImplTest {

    @Mock
    private UtilisateurRepository utilisateurRepository;

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Spy
    @InjectMocks
    private UtilisateurServiceImpl utilisateurService;


    @Test
    @DisplayName("Doit retourner un utilisateur existant par email")
    void testGetUtilisateurByEmail_success() {
        String email = "test@example.com";
        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setEmail(email);

        when(utilisateurRepository.findByEmail(email)).thenReturn(Optional.of(utilisateur));

        Utilisateur result = utilisateurService.getUtilisateurByEmail(email);

        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo(email);
    }

    @Test
    @DisplayName("Doit lancer UserNotFoundException si l'utilisateur n'existe pas")
    void testGetUtilisateurByEmail_notFound() {
        String email = "inconnu@example.com";
        when(utilisateurRepository.findByEmail(email)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> utilisateurService.getUtilisateurByEmail(email))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining(email);
    }


    @Test
    @DisplayName("Doit lancer UserAlreadyExistsException si l'utilisateur existe")
    void testVerifieSiUtilisateurEstPresent_exists() {
        String email = "test@example.com";
        when(utilisateurRepository.existsByEmail(email)).thenReturn(true);

        assertThatThrownBy(() -> utilisateurService.verifieSiUtilisateurEstPresent(email))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessageContaining(email);
    }

    @Test
    @DisplayName("Ne doit rien faire si l'utilisateur n'existe pas")
    void testVerifieSiUtilisateurEstPresent_notExists() {
        String email = "nouveau@example.com";
        when(utilisateurRepository.existsByEmail(email)).thenReturn(false);

        assertThatCode(() -> utilisateurService.verifieSiUtilisateurEstPresent(email))
                .doesNotThrowAnyException();
    }


    @Test
    @DisplayName("Doit sauvegarder un utilisateur via le repository")
    void testSave() {
        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setEmail("test@example.com");

        utilisateurService.save(utilisateur);

        verify(utilisateurRepository, times(1)).save(utilisateur);
    }


    @Test
    @DisplayName("Doit lancer InvalidRoleException si le rôle est null")
    void testVerifieSiRoleUtilisateurCorrect_null() {
        assertThatThrownBy(() -> utilisateurService.verifieSiRoleUtilisateurCorrect(null))
                .isInstanceOf(InvalidRoleException.class);
    }

    @Test
    @DisplayName("Ne doit rien faire si le rôle est correct")
    void testVerifieSiRoleUtilisateurCorrect_valid() {
        assertThatCode(() -> utilisateurService.verifieSiRoleUtilisateurCorrect(Role.valueOf("ROLE_LOCATAIRE")))
                .doesNotThrowAnyException();
    }


    @Test
    @DisplayName("Doit lancer InvalidRoleException si le rôle n'est pas locataire")
    void testVerifieSiUtilisateurEstLocataire_invalid() {
        String role = "ROLE_PROPRIETAIRE";
        assertThatThrownBy(() -> utilisateurService.verifieSiUtilisateurEstLocataire(Role.valueOf(role)))
                .isInstanceOf(InvalidRoleException.class)
                .hasMessageContaining("locataire");
    }

    @Test
    @DisplayName("Ne doit rien faire si le rôle est locataire")
    void testVerifieSiUtilisateurEstLocataire_valid() {
        assertThatCode(() -> utilisateurService.verifieSiUtilisateurEstLocataire(Role.ROLE_LOCATAIRE))
                .doesNotThrowAnyException();
    }


    @Test
    @DisplayName("Doit lancer InvalidRoleException si le rôle n'est pas propriétaire")
    void testVerifieSiUtilisateurEstProprietaire_invalid() {
        String role = "ROLE_LOCATAIRE";
        assertThatThrownBy(() -> utilisateurService.verifieSiUtilisateurEstProprietaire(Role.valueOf(role)))
                .isInstanceOf(InvalidRoleException.class)
                .hasMessageContaining("propriétaire");
    }

    @Test
    @DisplayName("Ne doit rien faire si le rôle est propriétaire")
    void testVerifieSiUtilisateurEstProprietaire_valid() {
        assertThatCode(() -> utilisateurService.verifieSiUtilisateurEstProprietaire(Role.ROLE_PROPRIETAIRE))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Login réussi : retourne AuthResponseDTO")
    void testLogin_success() {
        String email = "test@example.com";
        String motDePasse = "password123";

        LoginDTO loginDTO = new LoginDTO(email, motDePasse);
        Utilisateur utilisateur = Utilisateur.builder()
                .email(email)
                .motDePasse("encodedPassword")
                .role(Role.ROLE_LOCATAIRE)
                .build();

        // Mock des méthodes appelées dans login
        doReturn(utilisateur).when(utilisateurService).getUtilisateurByEmail(email);
        doNothing().when(utilisateurService).isCorrectPassword(motDePasse, utilisateur.getMotDePasse());
        when(jwtUtils.generateAccessToken(email)).thenReturn("jwt-token");
        when(jwtUtils.generateRefreshToken(email)).thenReturn("refresh-token");

        AuthResponseDTO response = utilisateurService.login(loginDTO);

        assertThat(response.email()).isEqualTo(email);
        assertThat(response.role()).isEqualTo(Role.ROLE_LOCATAIRE);
        assertThat(response.jwtToken()).isEqualTo("jwt-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
    }

    @Test
    @DisplayName("Utilisateur introuvable : UserNotFoundException")
    void testLogin_userNotFound() {
        String email = "inconnu@example.com";
        String motDePasse = "password123";
        LoginDTO loginDTO = new LoginDTO(email, motDePasse);

        // Ici on spy l'instance réelle et on force getUtilisateurByEmail à lancer l'exception
        doThrow(new UserNotFoundException(email))
                .when(utilisateurService).getUtilisateurByEmail(email);

        assertThatThrownBy(() -> utilisateurService.login(loginDTO))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining(email);
    }

    @Test
    @DisplayName("Mot de passe incorrect : exception")
    void testLogin_wrongPassword() {
        String email = "test@example.com";
        String motDePasse = "wrong-password";

        Utilisateur utilisateur = Utilisateur.builder()
                .email(email)
                .motDePasse("encodedPassword")
                .role(Role.ROLE_PROPRIETAIRE)
                .build();

        LoginDTO loginDTO = new LoginDTO(email, motDePasse);

        doReturn(utilisateur).when(utilisateurService).getUtilisateurByEmail(email);
        doThrow(new RuntimeException("Mot de passe incorrect"))
                .when(utilisateurService).isCorrectPassword(motDePasse, utilisateur.getMotDePasse());

        assertThatThrownBy(() -> utilisateurService.login(loginDTO))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Mot de passe incorrect");
    }
}
