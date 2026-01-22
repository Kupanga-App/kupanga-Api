package com.kupanga.api.login.service;

import com.kupanga.api.email.service.EmailService;
import com.kupanga.api.exception.business.InvalidRoleException;
import com.kupanga.api.exception.business.UserAlreadyExistsException;
import com.kupanga.api.login.service.impl.CreationCompteServiceImpl;
import com.kupanga.api.utilisateur.dto.readDTO.UtilisateurDTO;
import com.kupanga.api.utilisateur.entity.Role;
import com.kupanga.api.utilisateur.entity.Utilisateur;
import com.kupanga.api.utilisateur.mapper.UtilisateurMapper;
import com.kupanga.api.utilisateur.service.UtilisateurService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@DisplayName("Tests du service public CreationCompte")
class CreationCompteServiceImplTest {

    @Mock
    private UtilisateurService utilisateurService;

    @Mock
    private EmailService emailService;

    @Mock
    private UtilisateurMapper utilisateurMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private CreationCompteServiceImpl creationCompteService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("Doit créer un utilisateur avec rôle correct et envoyer email")
    void testCreationUtilisateurSuccess() throws UserAlreadyExistsException {
        String email = "test@example.com";
        Role role = Role.ROLE_LOCATAIRE;
        String motDePasseTemporaire = "temp1234";

        Utilisateur utilisateur = Utilisateur.builder()
                .email(email)
                .motDePasse("encodedPassword")
                .role(Role.ROLE_LOCATAIRE)
                .build();

        UtilisateurDTO utilisateurDTO = UtilisateurDTO.builder()
                .email(email)
                .role(role)
                .build();

        // Mocks
        doNothing().when(utilisateurService).verifieSiUtilisateurEstPresent(email);
        doNothing().when(utilisateurService).verifieSiRoleUtilisateurCorrect(role);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(utilisateurMapper.toDTO(any(Utilisateur.class))).thenReturn(utilisateurDTO);

        UtilisateurDTO result = creationCompteService.creationUtilisateur(email, role);

        // Vérifications
        assertThat(result.email()).isEqualTo(email);
        assertThat(result.role()).isEqualTo(role);
        verify(utilisateurService).save(any(Utilisateur.class));
        verify(emailService).envoyerMailMotDePasseTemporaire(eq(email), anyString());
    }

    @Test
    @DisplayName("Doit lancer UserAlreadyExistsException si l'utilisateur existe déjà")
    void testCreationUtilisateurUserAlreadyExists() throws UserAlreadyExistsException {
        String email = "exist@example.com";
        Role role = Role.ROLE_LOCATAIRE;

        doThrow(new UserAlreadyExistsException(email))
                .when(utilisateurService).verifieSiUtilisateurEstPresent(email);

        assertThrows(UserAlreadyExistsException.class, () ->
                creationCompteService.creationUtilisateur(email, role));

        verify(utilisateurService, never()).save(any());
        verify(emailService, never()).envoyerMailMotDePasseTemporaire(anyString(), anyString());
    }

    @Test
    @DisplayName("Doit lancer InvalidRoleException si rôle incorrect")
    void testCreationUtilisateurInvalidRole() throws UserAlreadyExistsException {
        String email = "test@example.com";

        // verifieSiUtilisateurEstPresent ne fait rien
        doNothing().when(utilisateurService).verifieSiUtilisateurEstPresent(email);

        // on force l'exception quand la méthode est appelée avec le rôle invalide
        doThrow(new InvalidRoleException())
                .when(utilisateurService).verifieSiRoleUtilisateurCorrect(any());

        // test la création du compte avec un rôle invalide
        assertThrows(InvalidRoleException.class, () ->
                creationCompteService.creationUtilisateur(email, any()));

        // vérifie que le reste n'a pas été exécuté
        verify(utilisateurService, never()).save(any());
        verify(emailService, never()).envoyerMailMotDePasseTemporaire(anyString(), anyString());
    }


    @Test
    @DisplayName("Doit continuer si l'envoi d'email échoue")
    void testCreationUtilisateurEmailException() throws UserAlreadyExistsException {
        String email = "test@example.com";
        String role = "ROLE_LOCATAIRE";

        doNothing().when(utilisateurService).verifieSiUtilisateurEstPresent(email);
        doNothing().when(utilisateurService).verifieSiRoleUtilisateurCorrect(Role.valueOf(role));
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(utilisateurMapper.toDTO(any(Utilisateur.class))).thenReturn(UtilisateurDTO.builder().build());

        // Simule une exception sur l'envoi d'email
        doThrow(new RuntimeException("Erreur email"))
                .when(emailService).envoyerMailMotDePasseTemporaire(anyString(), anyString());

        UtilisateurDTO result = creationCompteService.creationUtilisateur(email, Role.valueOf(role));

        assertThat(result).isNotNull();
        verify(utilisateurService).save(any(Utilisateur.class));
        verify(emailService).envoyerMailMotDePasseTemporaire(eq(email), anyString());
    }
}
