package com.kupanga.api.login.service;

import com.kupanga.api.login.service.impl.UserDetailsServiceImpl;
import com.kupanga.api.utilisateur.entity.Role;
import com.kupanga.api.utilisateur.entity.User;
import com.kupanga.api.utilisateur.repository.UserRepository;
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
    private UserRepository userRepository;

    @InjectMocks
    private UserDetailsServiceImpl userDetailsService;

    private User locataire;
    private User proprietaire;

    @BeforeEach
    void setUp() {
        // Initialisation avec builder
        locataire = User.builder()
                .mail("locataire@mail.com")
                .password("encodedPassword")
                .role(Role.ROLE_LOCATAIRE)
                .build();

        proprietaire = User.builder()
                .mail("proprietaire@mail.com")
                .password("encodedPassword")
                .role(Role.ROLE_PROPRIETAIRE)
                .build();
    }

    @Test
    @DisplayName("Chargement d'un utilisateur LOCATAIRE")
    void shouldLoadLocataireUser() {
        when(userRepository.findByMail("locataire@mail.com"))
                .thenReturn(Optional.of(locataire));

        UserDetails userDetails = userDetailsService.loadUserByUsername("locataire@mail.com");

        assertEquals("locataire@mail.com", userDetails.getUsername());
        assertEquals("encodedPassword", userDetails.getPassword());
        assertTrue(userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_LOCATAIRE"))
        );

        verify(userRepository, times(1)).findByMail("locataire@mail.com");
    }

    @Test
    @DisplayName("Chargement d'un utilisateur PROPRIETAIRE")
    void shouldLoadProprietaireUser() {
        when(userRepository.findByMail("proprietaire@mail.com"))
                .thenReturn(Optional.of(proprietaire));

        UserDetails userDetails = userDetailsService.loadUserByUsername("proprietaire@mail.com");

        assertEquals("proprietaire@mail.com", userDetails.getUsername());
        assertTrue(userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_PROPRIETAIRE"))
        );

        verify(userRepository, times(1)).findByMail("proprietaire@mail.com");
    }

    @Test
    @DisplayName("Exception levée lorsque l'utilisateur n'existe pas")
    void shouldThrowExceptionWhenUserNotFound() {
        when(userRepository.findByMail("unknown@mail.com"))
                .thenReturn(Optional.empty());

        UsernameNotFoundException exception = assertThrows(
                UsernameNotFoundException.class,
                () -> userDetailsService.loadUserByUsername("unknown@mail.com")
        );

        assertEquals("Utilisateur non trouvé avec l'email : unknown@mail.com", exception.getMessage());
        verify(userRepository, times(1)).findByMail("unknown@mail.com");
    }

    @Test
    @DisplayName("Exception levée si le rôle est inconnu")
    void shouldThrowExceptionWhenRoleUnknown() {
        // Création d'un utilisateur avec un rôle null (inconnu)
        User inconnu = User.builder()
                .mail("inconnu@mail.com")
                .password("encodedPassword")
                .role(null)  // rôle inconnu
                .build();

        when(userRepository.findByMail("inconnu@mail.com"))
                .thenReturn(Optional.of(inconnu));

        // Ici on peut lever IllegalArgumentException ou UsernameNotFoundException selon ton choix
        Exception exception = assertThrows(
                IllegalArgumentException.class,
                () -> userDetailsService.loadUserByUsername("inconnu@mail.com")
        );

        assertEquals("L'utilisateur a un rôle invalide", exception.getMessage());
    }
}



