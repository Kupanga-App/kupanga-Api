package com.kupanga.api.utilisateur.service;

import com.kupanga.api.exception.business.InvalidRoleException;
import com.kupanga.api.exception.business.UserAlreadyExistsException;
import com.kupanga.api.exception.business.UserNotFoundException;
import com.kupanga.api.utilisateur.entity.Role;
import com.kupanga.api.utilisateur.entity.Utilisateur;
import com.kupanga.api.utilisateur.repository.UtilisateurRepository;
import com.kupanga.api.utilisateur.service.impl.UtilisateurServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("Tests pour le service utilisateur")
class UtilisateurServiceImplTest {

    @Mock
    private UtilisateurRepository utilisateurRepository;

    @InjectMocks
    private UtilisateurServiceImpl utilisateurService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    // -----------------------
    // Test getUtilisateurByEmail
    // -----------------------
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

    // -----------------------
    // Test verifieSiUtilisateurEstPresent
    // -----------------------
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

    // -----------------------
    // Test save
    // -----------------------
    @Test
    @DisplayName("Doit sauvegarder un utilisateur via le repository")
    void testSave() {
        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setEmail("test@example.com");

        utilisateurService.save(utilisateur);

        verify(utilisateurRepository, times(1)).save(utilisateur);
    }

    // -----------------------
    // Test verifieSiRoleUtilisateurCorrect
    // -----------------------
    @Test
    @DisplayName("Doit lancer InvalidRoleException si le rôle est null")
    void testVerifieSiRoleUtilisateurCorrect_null() {
        assertThatThrownBy(() -> utilisateurService.verifieSiRoleUtilisateurCorrect(null))
                .isInstanceOf(InvalidRoleException.class);
    }

    @Test
    @DisplayName("Doit lancer InvalidRoleException si le rôle n'existe pas")
    void testVerifieSiRoleUtilisateurCorrect_invalid() {
        assertThatThrownBy(() -> utilisateurService.verifieSiRoleUtilisateurCorrect("ROLE_INVALIDE"))
                .isInstanceOf(InvalidRoleException.class);
    }

    @Test
    @DisplayName("Ne doit rien faire si le rôle est correct")
    void testVerifieSiRoleUtilisateurCorrect_valid() {
        assertThatCode(() -> utilisateurService.verifieSiRoleUtilisateurCorrect("ROLE_LOCATAIRE"))
                .doesNotThrowAnyException();
    }

    // -----------------------
    // Test verifieSiUtilisateurEstLocataire
    // -----------------------
    @Test
    @DisplayName("Doit lancer InvalidRoleException si le rôle n'est pas locataire")
    void testVerifieSiUtilisateurEstLocataire_invalid() {
        String role = "ROLE_PROPRIETAIRE";
        assertThatThrownBy(() -> utilisateurService.verifieSiUtilisateurEstLocataire(role))
                .isInstanceOf(InvalidRoleException.class)
                .hasMessageContaining("locataire");
    }

    @Test
    @DisplayName("Ne doit rien faire si le rôle est locataire")
    void testVerifieSiUtilisateurEstLocataire_valid() {
        assertThatCode(() -> utilisateurService.verifieSiUtilisateurEstLocataire(Role.ROLE_LOCATAIRE.name()))
                .doesNotThrowAnyException();
    }

    // -----------------------
    // Test verifieSiUtilisateurEstProprietaire
    // -----------------------
    @Test
    @DisplayName("Doit lancer InvalidRoleException si le rôle n'est pas propriétaire")
    void testVerifieSiUtilisateurEstProprietaire_invalid() {
        String role = "ROLE_LOCATAIRE";
        assertThatThrownBy(() -> utilisateurService.verifieSiUtilisateurEstProprietaire(role))
                .isInstanceOf(InvalidRoleException.class)
                .hasMessageContaining("propriétaire");
    }

    @Test
    @DisplayName("Ne doit rien faire si le rôle est propriétaire")
    void testVerifieSiUtilisateurEstProprietaire_valid() {
        assertThatCode(() -> utilisateurService.verifieSiUtilisateurEstProprietaire(Role.ROLE_PROPRIETAIRE.name()))
                .doesNotThrowAnyException();
    }
}
