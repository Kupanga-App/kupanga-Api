package com.kupanga.api.login.service;

import com.kupanga.api.login.service.impl.UserDetailsServiceImpl;
import com.kupanga.api.utilisateur.entity.Role;
import com.kupanga.api.utilisateur.entity.Utilisateur;
import com.kupanga.api.utilisateur.repository.UtilisateurRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires de {@link UserDetailsServiceImpl}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Tests unitaires pour UserDetailsServiceImpl")
class UserDetailsServiceImplTest {

    @Mock
    private UtilisateurRepository utilisateurRepository;

    @InjectMocks
    private UserDetailsServiceImpl userDetailsService;

    private Utilisateur locataire;
    private Utilisateur proprietaire;

    @BeforeEach
    void setUp() {
        // Initialisation avec builder
        locataire = Utilisateur.builder()
                .email("locataire@mail.com")
                .motDePasse("encodedPassword")
                .role(Role.ROLE_LOCATAIRE)
                .build();

        proprietaire = Utilisateur.builder()
                .email("proprietaire@mail.com")
                .motDePasse("encodedPassword")
                .role(Role.ROLE_PROPRIETAIRE)
                .build();
    }

    @Test
    @DisplayName("Chargement d'un utilisateur LOCATAIRE")
    void shouldLoadLocataireUser() {
        when(utilisateurRepository.findByEmail("locataire@mail.com"))
                .thenReturn(Optional.of(locataire));

        UserDetails userDetails = userDetailsService.loadUserByUsername("locataire@mail.com");

        assertEquals("locataire@mail.com", userDetails.getUsername());
        assertEquals("encodedPassword", userDetails.getPassword());
        assertTrue(userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_LOCATAIRE"))
        );

        verify(utilisateurRepository, times(1)).findByEmail("locataire@mail.com");
    }

    @Test
    @DisplayName("Chargement d'un utilisateur PROPRIETAIRE")
    void shouldLoadProprietaireUser() {
        when(utilisateurRepository.findByEmail("proprietaire@mail.com"))
                .thenReturn(Optional.of(proprietaire));

        UserDetails userDetails = userDetailsService.loadUserByUsername("proprietaire@mail.com");

        assertEquals("proprietaire@mail.com", userDetails.getUsername());
        assertTrue(userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_PROPRIETAIRE"))
        );

        verify(utilisateurRepository, times(1)).findByEmail("proprietaire@mail.com");
    }

    @Test
    @DisplayName("Exception levée lorsque l'utilisateur n'existe pas")
    void shouldThrowExceptionWhenUserNotFound() {
        when(utilisateurRepository.findByEmail("unknown@mail.com"))
                .thenReturn(Optional.empty());

        UsernameNotFoundException exception = assertThrows(
                UsernameNotFoundException.class,
                () -> userDetailsService.loadUserByUsername("unknown@mail.com")
        );

        assertEquals("Utilisateur non trouvé avec l'email : unknown@mail.com", exception.getMessage());
        verify(utilisateurRepository, times(1)).findByEmail("unknown@mail.com");
    }

    @Test
    @DisplayName("Exception levée si le rôle est inconnu")
    void shouldThrowExceptionWhenRoleUnknown() {
        // Création d'un utilisateur avec un rôle null (inconnu)
        Utilisateur inconnu = Utilisateur.builder()
                .email("inconnu@mail.com")
                .motDePasse("encodedPassword")
                .role(null)  // rôle inconnu
                .build();

        when(utilisateurRepository.findByEmail("inconnu@mail.com"))
                .thenReturn(Optional.of(inconnu));

        // Ici on peut lever IllegalArgumentException ou UsernameNotFoundException selon ton choix
        Exception exception = assertThrows(
                IllegalArgumentException.class,
                () -> userDetailsService.loadUserByUsername("inconnu@mail.com")
        );

        assertEquals("L'utilisateur a un rôle invalide", exception.getMessage());
    }
}



