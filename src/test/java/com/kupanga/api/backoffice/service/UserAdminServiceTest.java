package com.kupanga.api.backoffice.service;

import com.kupanga.api.authentification.entity.RefreshToken;
import com.kupanga.api.authentification.repository.JetonVerificationEmailRepository;
import com.kupanga.api.authentification.repository.PasswordResetTokenRepository;
import com.kupanga.api.authentification.repository.RefreshTokenRepository;
import com.kupanga.api.notification.repository.NotificationRepository;
import com.kupanga.api.backoffice.dto.UserAdminPageDTO;
import com.kupanga.api.backoffice.dto.UserAdminSearchDTO;
import com.kupanga.api.backoffice.specification.UserAdminSpecification;
import com.kupanga.api.chat.repository.ConversationRepository;
import com.kupanga.api.chat.repository.MessageRepository;
import com.kupanga.api.immobilier.repository.BienRepository;
import com.kupanga.api.immobilier.repository.ContratRepository;
import com.kupanga.api.immobilier.repository.EtatDesLieuxRepository;
import com.kupanga.api.immobilier.entity.StatutContrat;
import com.kupanga.api.immobilier.entity.StatutEdl;
import com.kupanga.api.immobilier.repository.QuittanceRepository;
import com.kupanga.api.minio.service.MinioService;
import com.kupanga.api.user.entity.Role;
import com.kupanga.api.user.entity.User;
import com.kupanga.api.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@DisplayName("Tests unitaires — UserAdminService")
@SuppressWarnings("unchecked")
class UserAdminServiceTest {

    @Mock private UserRepository                userRepository;
    @Mock private UserAdminSpecification        userAdminSpecification;
    @Mock private RefreshTokenRepository        refreshTokenRepository;
    @Mock private PasswordResetTokenRepository  passwordResetTokenRepository;
    @Mock private NotificationRepository        notificationRepository;
    @Mock private JetonVerificationEmailRepository jetonVerificationEmailRepository;
    @Mock private BienRepository                bienRepository;
    @Mock private ContratRepository             contratRepository;
    @Mock private QuittanceRepository           quittanceRepository;
    @Mock private EtatDesLieuxRepository        etatDesLieuxRepository;
    @Mock private MessageRepository             messageRepository;
    @Mock private ConversationRepository        conversationRepository;
    @Mock private MinioService                  minioService;

    @InjectMocks
    private UserAdminService userAdminService;

    private User user;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        user = User.builder().id(1L).mail("alice@test.com").role(Role.ROLE_PROPRIETAIRE).build();
    }

    @Test
    @DisplayName("rechercher() — retourne une page de UserAdminDTO")
    void rechercher_returnsPage() {
        UserAdminSearchDTO dto = new UserAdminSearchDTO(null, null, null, null, 0, 10);

        when(userAdminSpecification.build(dto)).thenReturn(mock(Specification.class));
        when(userRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(user)));

        UserAdminPageDTO result = userAdminService.rechercher(dto);

        assertThat(result).isNotNull();
        assertThat(result.contenu()).hasSize(1);
    }

    @Test
    @DisplayName("B12 : supprimer() — sans donnée liée → jetons, notifications, messages, conversations puis compte supprimés")
    void supprimer_sansDonneesLiees_supprime() {
        RefreshToken refreshToken = mock(RefreshToken.class);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(refreshTokenRepository.findByUserId(1L)).thenReturn(refreshToken);

        assertThat(userAdminService.supprimer(1L)).isEqualTo(ResultatSuppressionCompte.SUPPRIME);

        InOrder order = inOrder(refreshTokenRepository, passwordResetTokenRepository, notificationRepository,
                messageRepository, conversationRepository, userRepository);
        order.verify(refreshTokenRepository).delete(refreshToken);
        order.verify(passwordResetTokenRepository).deleteByUserId(1L);
        order.verify(notificationRepository).deleteByDestinataireId(1L);
        order.verify(messageRepository).supprimerParUtilisateur(1L);
        order.verify(conversationRepository).supprimerParEmail("alice@test.com");
        order.verify(userRepository).deleteById(1L);
        verify(jetonVerificationEmailRepository).deleteByUserId(1L);
        verify(bienRepository, never()).archiverParProprietaire(anyLong(), any());
        verifyNoInteractions(minioService);
    }

    @Test
    @DisplayName("B12 : supprimer() — photo de profil supprimée de MinIO, sauf si un autre compte l'utilise")
    void supprimer_photoDeProfil() {
        user.setUrlProfile("http://minio/bucket-photo-profil/a.jpg");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        userAdminService.supprimer(1L);
        verify(minioService).supprimerParUrl("http://minio/bucket-photo-profil/a.jpg", "bucket-photo-profil");

        User autre = User.builder().id(2L).mail("bob@test.com").urlProfile("http://minio/bucket-photo-profil/b.jpg").build();
        when(userRepository.findById(2L)).thenReturn(Optional.of(autre));
        when(userRepository.existsByUrlProfileAndIdNot("http://minio/bucket-photo-profil/b.jpg", 2L)).thenReturn(true);

        userAdminService.supprimer(2L);
        verify(minioService, never()).supprimerParUrl(eq("http://minio/bucket-photo-profil/b.jpg"), anyString());
    }

    @Test
    @DisplayName("B12 : supprimer() — avec un bail → compte anonymisé, jamais supprimé ; biens archivés, documents conservés")
    void supprimer_avecDonneesLiees_anonymise() {
        user.setFirstName("Alice");
        user.setLastName("Martin");
        user.setPassword("hash-bcrypt");
        user.setGoogleId("google-123");
        user.setUrlProfile("http://minio/photo.jpg");
        user.setEmailVerifie(true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(contratRepository.existsByProprietaire_IdOrLocataire_Id(1L, 1L)).thenReturn(true);

        assertThat(userAdminService.supprimer(1L)).isEqualTo(ResultatSuppressionCompte.ANONYMISE);

        verify(userRepository, never()).deleteById(any());
        verify(userRepository, never()).delete(any(User.class));
        verify(messageRepository, never()).supprimerParUtilisateur(anyLong());
        verify(conversationRepository, never()).supprimerParEmail(anyString());
        verify(bienRepository).archiverParProprietaire(eq(1L), any());
        verify(bienRepository).retirerLocataire(1L);
        ArgumentCaptor<String> mailAnonyme = ArgumentCaptor.forClass(String.class);
        verify(conversationRepository).remplacerEmail(eq("alice@test.com"), mailAnonyme.capture());
        verify(contratRepository).expirerNonSignesDeUtilisateur(1L, StatutContrat.SIGNE, StatutContrat.EXPIRE);
        verify(etatDesLieuxRepository).expirerNonSignesDeUtilisateur(1L, StatutEdl.SIGNE, StatutEdl.EXPIRE);
        verify(minioService).supprimerParUrl("http://minio/photo.jpg", "bucket-photo-profil");
        verify(notificationRepository).deleteByDestinataireId(1L);
        verify(passwordResetTokenRepository).deleteByUserId(1L);
        verify(userRepository).save(user);

        assertThat(user.isAnonymise()).isTrue();
        assertThat(user.getDateAnonymisation()).isNotNull();
        // Partie locale aléatoire : un tiers ne peut pas inscrire l'adresse à l'avance
        assertThat(user.getMail()).isEqualTo(mailAnonyme.getValue())
                .matches("supprime-[0-9a-f-]{36}@anonyme\\.invalid");
        assertThat(user.getFirstName()).isEqualTo("Utilisateur");
        assertThat(user.getLastName()).isEqualTo("supprimé");
        assertThat(user.getPassword()).isNull();
        assertThat(user.getGoogleId()).isNull();
        assertThat(user.getUrlProfile()).isNull();
        assertThat(user.isEmailVerifie()).isFalse();
    }

    @Test
    @DisplayName("B12 : supprimer() — propriétaire d'un bien sans document → anonymisé aussi (le bien est archivé)")
    void supprimer_proprietaireSansDocument_anonymise() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(bienRepository.existsByProprietaire_IdOrLocataire_Id(1L, 1L)).thenReturn(true);

        assertThat(userAdminService.supprimer(1L)).isEqualTo(ResultatSuppressionCompte.ANONYMISE);

        verify(bienRepository).archiverParProprietaire(eq(1L), any());
        verify(userRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("B12 : supprimer() — compte déjà anonymisé ou introuvable → rien n'est modifié")
    void supprimer_dejaAnonymiseOuIntrouvable_rienNeChange() {
        user.setAnonymise(true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.findById(2L)).thenReturn(Optional.empty());

        assertThat(userAdminService.supprimer(1L)).isEqualTo(ResultatSuppressionCompte.DEJA_ANONYMISE);
        assertThat(userAdminService.supprimer(2L)).isEqualTo(ResultatSuppressionCompte.INTROUVABLE);

        verify(userRepository, never()).deleteById(any());
        verify(userRepository, never()).save(any());
        verifyNoInteractions(notificationRepository, messageRepository, conversationRepository, bienRepository);
    }

    @Test
    @DisplayName("countAll() — délègue count au repository")
    void countAll_delegatesToRepository() {
        when(userRepository.count()).thenReturn(12L);

        assertThat(userAdminService.countAll()).isEqualTo(12L);
    }

    @Test
    @DisplayName("countByRole() — délègue countByRole au repository")
    void countByRole_delegatesToRepository() {
        when(userRepository.countByRole(Role.ROLE_PROPRIETAIRE)).thenReturn(4L);

        assertThat(userAdminService.countByRole(Role.ROLE_PROPRIETAIRE)).isEqualTo(4L);
    }

    @Test
    @DisplayName("rechercher() — page vide → contenu vide")
    void rechercher_emptyPage_returnsEmptyContent() {
        UserAdminSearchDTO dto = new UserAdminSearchDTO(null, null, null, null, 0, 10);

        when(userAdminSpecification.build(dto)).thenReturn(mock(Specification.class));
        when(userRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        UserAdminPageDTO result = userAdminService.rechercher(dto);

        assertThat(result.contenu()).isEmpty();
    }
}
