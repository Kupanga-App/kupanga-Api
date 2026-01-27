package com.kupanga.api.utilisateur.service;

import com.kupanga.api.exception.business.InvalidRoleException;
import com.kupanga.api.exception.business.UserAlreadyExistsException;
import com.kupanga.api.exception.business.UserNotFoundException;
import com.kupanga.api.authentification.utils.JwtUtils;
import com.kupanga.api.utilisateur.entity.Role;
import com.kupanga.api.utilisateur.entity.User;
import com.kupanga.api.utilisateur.repository.UserRepository;
import com.kupanga.api.utilisateur.service.impl.UserServiceImpl;
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
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Spy
    @InjectMocks
    private UserServiceImpl utilisateurService;


    @Test
    @DisplayName("Doit retourner un utilisateur existant par email")
    void testGetUtilisateurByEmail_success() {
        String email = "test@example.com";
        User utilisateur = new User();
        utilisateur.setMail(email);

        when(userRepository.findByMail(email)).thenReturn(Optional.of(utilisateur));

        User result = utilisateurService.getUserByEmail(email);

        assertThat(result).isNotNull();
        assertThat(result.getMail()).isEqualTo(email);
    }

    @Test
    @DisplayName("Doit lancer UserNotFoundException si l'utilisateur n'existe pas")
    void testGetUtilisateurByEmail_notFound() {
        String email = "inconnu@example.com";
        when(userRepository.findByMail(email)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> utilisateurService.getUserByEmail(email))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining(email);
    }


    @Test
    @DisplayName("Doit lancer UserAlreadyExistsException si l'utilisateur existe")
    void testVerifieSiUtilisateurEstPresent_exists() {
        String email = "test@example.com";
        when(userRepository.existsByMail(email)).thenReturn(true);

        assertThatThrownBy(() -> utilisateurService.verifyIfUserExistWithEmail(email))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessageContaining(email);
    }

    @Test
    @DisplayName("Ne doit rien faire si l'utilisateur n'existe pas")
    void testVerifieSiUtilisateurEstPresent_notExists() {
        String email = "nouveau@example.com";
        when(userRepository.existsByMail(email)).thenReturn(false);

        assertThatCode(() -> utilisateurService.verifyIfUserExistWithEmail(email))
                .doesNotThrowAnyException();
    }


    @Test
    @DisplayName("Doit sauvegarder un utilisateur via le repository")
    void testSave() {
        User utilisateur = new User();
        utilisateur.setMail("test@example.com");

        utilisateurService.save(utilisateur);

        verify(userRepository, times(1)).save(utilisateur);
    }


    @Test
    @DisplayName("Doit lancer InvalidRoleException si le rôle est null")
    void testVerifieSiRoleUtilisateurCorrect_null() {
        assertThatThrownBy(() -> utilisateurService.verifyIfRoleOfUserValid(null))
                .isInstanceOf(InvalidRoleException.class);
    }

    @Test
    @DisplayName("Ne doit rien faire si le rôle est correct")
    void testVerifieSiRoleUtilisateurCorrect_valid() {
        assertThatCode(() -> utilisateurService.verifyIfRoleOfUserValid(Role.valueOf("ROLE_LOCATAIRE")))
                .doesNotThrowAnyException();
    }


    @Test
    @DisplayName("Doit lancer InvalidRoleException si le rôle n'est pas locataire")
    void testVerifieSiUtilisateurEstLocataire_invalid() {
        String role = "ROLE_PROPRIETAIRE";
        assertThatThrownBy(() -> utilisateurService.verifyIfUserIsTenant(Role.valueOf(role)))
                .isInstanceOf(InvalidRoleException.class)
                .hasMessageContaining("locataire");
    }

    @Test
    @DisplayName("Ne doit rien faire si le rôle est locataire")
    void testVerifieSiUtilisateurEstLocataire_valid() {
        assertThatCode(() -> utilisateurService.verifyIfUserIsTenant(Role.ROLE_LOCATAIRE))
                .doesNotThrowAnyException();
    }


    @Test
    @DisplayName("Doit lancer InvalidRoleException si le rôle n'est pas propriétaire")
    void testVerifieSiUtilisateurEstProprietaire_invalid() {
        String role = "ROLE_LOCATAIRE";
        assertThatThrownBy(() -> utilisateurService.verifyIfUserIsOwner(Role.valueOf(role)))
                .isInstanceOf(InvalidRoleException.class)
                .hasMessageContaining("propriétaire");
    }

    @Test
    @DisplayName("Ne doit rien faire si le rôle est propriétaire")
    void testVerifieSiUtilisateurEstProprietaire_valid() {
        assertThatCode(() -> utilisateurService.verifyIfUserIsOwner(Role.ROLE_PROPRIETAIRE))
                .doesNotThrowAnyException();
    }
}
