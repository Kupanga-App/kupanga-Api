package com.kupanga.api.immobilier.service;

import com.kupanga.api.chat.entity.Conversation;
import com.kupanga.api.chat.repository.ConversationRepository;
import com.kupanga.api.email.service.EmailService;
import com.kupanga.api.exception.business.KupangaBusinessException;
import com.kupanga.api.immobilier.dto.readDTO.DemandeLocationDTO;
import com.kupanga.api.immobilier.entity.Bien;
import com.kupanga.api.immobilier.entity.DemandeLocation;
import com.kupanga.api.immobilier.entity.StatutDemandeLocation;
import com.kupanga.api.immobilier.repository.BienRepository;
import com.kupanga.api.immobilier.repository.DemandeLocationRepository;
import com.kupanga.api.immobilier.service.impl.DemandeLocationServiceImplAccess;
import com.kupanga.api.juridiction.Pays;
import com.kupanga.api.notification.enums.NotificationType;
import com.kupanga.api.notification.service.NotificationService;
import com.kupanga.api.user.entity.Role;
import com.kupanga.api.user.entity.User;
import com.kupanga.api.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DemandeLocationService : le bien n'est assigné qu'avec l'accord du locataire")
class DemandeLocationServiceImplTest {

    private static final ZoneId ZONE = ZoneId.of("Europe/Paris");
    private static final Instant INSTANT = Instant.parse("2026-11-03T10:00:00Z");
    private static final LocalDateTime MAINTENANT = LocalDateTime.ofInstant(INSTANT, ZONE);

    @Mock private DemandeLocationRepository demandeRepository;
    @Mock private BienRepository bienRepository;
    @Mock private ConversationRepository conversationRepository;
    @Mock private BienService bienService;
    @Mock private UserService userService;
    @Mock private NotificationService notificationService;
    @Mock private EmailService emailService;

    private DemandeLocationService service;
    private User proprietaire;
    private User locataire;
    private Bien bien;

    @BeforeEach
    void setUp() {
        service = DemandeLocationServiceImplAccess.creer(demandeRepository, bienRepository, conversationRepository,
                bienService, userService, notificationService, emailService, Clock.fixed(INSTANT, ZONE));
        proprietaire = User.builder().id(1L).firstName("Paul").lastName("Proprio").mail("proprio@test.fr")
                .role(Role.ROLE_PROPRIETAIRE).build();
        locataire = User.builder().id(2L).firstName("Lea").lastName("Loc").mail("loc@test.fr")
                .role(Role.ROLE_LOCATAIRE).build();
        bien = Bien.builder().id(10L).titre("T2").adresse("12 rue A").codePostal("44000").ville("Nantes")
                .pays(Pays.FR).proprietaire(proprietaire).build();
    }

    private void proprietaireConnecte() {
        when(userService.getUserByEmail("proprio@test.fr")).thenReturn(proprietaire);
        when(bienService.verifierProprietaire(10L, "proprio@test.fr")).thenReturn(bien);
    }

    private void candidat() {
        when(userService.findById(2L)).thenReturn(locataire);
        when(conversationRepository.findConversationWithBienIdAndEmailExpediteur(10L, "proprio@test.fr", "loc@test.fr"))
                .thenReturn(Optional.of(new Conversation()));
    }

    private DemandeLocation demande(StatutDemandeLocation statut, LocalDateTime expiration) {
        return DemandeLocation.builder().id(50L).bien(bien).proprietaire(proprietaire).locataire(locataire)
                .statut(statut).dateCreation(expiration.minusHours(72)).dateExpiration(expiration).build();
    }

    // ── Envoi ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("envoyer : demande en attente 72 h, notification + e-mail au locataire, bien NON assigné")
    void envoyer_creeLaDemandeSansAssigner() {
        proprietaireConnecte();
        candidat();
        when(demandeRepository.saveAndFlush(any())).thenAnswer(i -> {
            DemandeLocation d = i.getArgument(0);
            d.setId(50L);
            return d;
        });

        DemandeLocationDTO dto = service.envoyer("proprio@test.fr", 10L, 2L);

        assertThat(dto.statut()).isEqualTo(StatutDemandeLocation.EN_ATTENTE);
        assertThat(dto.dateExpiration()).isEqualTo(MAINTENANT.plusHours(72));
        assertThat(bien.getLocataire()).isNull();
        verify(bienRepository, never()).save(any());
        verify(demandeRepository).expirerEchuesDuBien(10L, MAINTENANT);
        verify(notificationService).saveAndSend(eq(locataire), eq(NotificationType.DEMANDE_LOCATION),
                anyString(), anyString(), isNull(), eq(50L));
        verify(emailService).envoyerDemandeLocation(any(DemandeLocation.class));
    }

    @Test
    @DisplayName("envoyer : bien déjà loué → 409, rien n'est envoyé")
    void envoyer_bienLoue_409() {
        bien.setLocataire(User.builder().id(7L).build());
        proprietaireConnecte();

        assertThatThrownBy(() -> service.envoyer("proprio@test.fr", 10L, 2L))
                .extracting("status").isEqualTo(HttpStatus.CONFLICT);
        verify(demandeRepository, never()).saveAndFlush(any());
        verifyNoInteractions(notificationService, emailService);
    }

    @Test
    @DisplayName("envoyer : une demande déjà en attente sur le bien → 409")
    void envoyer_dejaEnAttente_409() {
        proprietaireConnecte();
        candidat();
        when(demandeRepository.existsByBien_IdAndStatut(10L, StatutDemandeLocation.EN_ATTENTE)).thenReturn(true);

        assertThatThrownBy(() -> service.envoyer("proprio@test.fr", 10L, 2L))
                .extracting("status").isEqualTo(HttpStatus.CONFLICT);
        verify(demandeRepository, never()).saveAndFlush(any());
        verifyNoInteractions(notificationService, emailService);
    }

    @Test
    @DisplayName("envoyer : deux envois simultanés (index unique V53) → 409 et non 500")
    void envoyer_concurrence_409() {
        proprietaireConnecte();
        candidat();
        when(demandeRepository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("ux"));

        assertThatThrownBy(() -> service.envoyer("proprio@test.fr", 10L, 2L))
                .extracting("status").isEqualTo(HttpStatus.CONFLICT);
        verifyNoInteractions(notificationService, emailService);
    }

    @Test
    @DisplayName("envoyer : non-candidat, compte anonymisé et id inconnu → même 404 (pas d'énumération)")
    void envoyer_nonCandidat_404() {
        proprietaireConnecte();
        when(userService.findById(2L)).thenReturn(locataire);
        when(conversationRepository.findConversationWithBienIdAndEmailExpediteur(10L, "proprio@test.fr", "loc@test.fr"))
                .thenReturn(Optional.empty());
        when(userService.findById(999L)).thenThrow(new KupangaBusinessException("Aucun utilisateur", HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> service.envoyer("proprio@test.fr", 10L, 2L))
                .hasMessage("Locataire introuvable pour ce bien").extracting("status").isEqualTo(HttpStatus.NOT_FOUND);
        assertThatThrownBy(() -> service.envoyer("proprio@test.fr", 10L, 999L))
                .hasMessage("Locataire introuvable pour ce bien");
        locataire.setAnonymise(true);
        assertThatThrownBy(() -> service.envoyer("proprio@test.fr", 10L, 2L))
                .hasMessage("Locataire introuvable pour ce bien");
        verify(demandeRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("envoyer : compte non locataire → 400 ; bien archivé → 409 (vérification du BienService)")
    void envoyer_refusDivers() {
        proprietaireConnecte();
        User autreProprio = User.builder().id(3L).mail("p2@test.fr").role(Role.ROLE_PROPRIETAIRE).build();
        when(userService.findById(3L)).thenReturn(autreProprio);
        when(conversationRepository.findConversationWithBienIdAndEmailExpediteur(10L, "proprio@test.fr", "p2@test.fr"))
                .thenReturn(Optional.of(new Conversation()));
        assertThatThrownBy(() -> service.envoyer("proprio@test.fr", 10L, 3L))
                .extracting("status").isEqualTo(HttpStatus.BAD_REQUEST);

        doThrow(new KupangaBusinessException("Ce bien est archivé", HttpStatus.CONFLICT))
                .when(bienService).verifierBienActif(bien);
        assertThatThrownBy(() -> service.envoyer("proprio@test.fr", 10L, 2L))
                .extracting("status").isEqualTo(HttpStatus.CONFLICT);
        verify(demandeRepository, never()).saveAndFlush(any());
    }

    // ── Réponse du locataire ─────────────────────────────────────────────────

    @Test
    @DisplayName("accepter : le bien est assigné, le propriétaire est prévenu")
    void accepter_assigneLeBien() {
        DemandeLocation d = demande(StatutDemandeLocation.EN_ATTENTE, MAINTENANT.plusHours(5));
        when(demandeRepository.findAvecParties(50L)).thenReturn(Optional.of(d));

        DemandeLocationDTO dto = service.accepter("loc@test.fr", 50L);

        assertThat(dto.statut()).isEqualTo(StatutDemandeLocation.ACCEPTEE);
        assertThat(d.getDateReponse()).isEqualTo(MAINTENANT);
        assertThat(bien.getLocataire()).isEqualTo(locataire);
        verify(bienRepository).save(bien);
        verify(notificationService).saveAndSend(eq(proprietaire), eq(NotificationType.DEMANDE_LOCATION_ACCEPTEE),
                anyString(), anyString(), isNull(), eq(10L));
    }

    @Test
    @DisplayName("accepter : demande expirée → 410 ; déjà traitée → 409 ; adressée à un autre → 404 ; bien loué entre-temps → 409")
    void accepter_refus() {
        when(demandeRepository.findAvecParties(50L))
                .thenReturn(Optional.of(demande(StatutDemandeLocation.EN_ATTENTE, MAINTENANT)));
        assertThatThrownBy(() -> service.accepter("loc@test.fr", 50L)).extracting("status").isEqualTo(HttpStatus.GONE);

        when(demandeRepository.findAvecParties(50L))
                .thenReturn(Optional.of(demande(StatutDemandeLocation.ANNULEE, MAINTENANT.plusHours(5))));
        assertThatThrownBy(() -> service.accepter("loc@test.fr", 50L)).extracting("status").isEqualTo(HttpStatus.CONFLICT);

        when(demandeRepository.findAvecParties(50L))
                .thenReturn(Optional.of(demande(StatutDemandeLocation.EN_ATTENTE, MAINTENANT.plusHours(5))));
        assertThatThrownBy(() -> service.accepter("intrus@test.fr", 50L)).extracting("status").isEqualTo(HttpStatus.NOT_FOUND);

        bien.setLocataire(User.builder().id(7L).build());
        assertThatThrownBy(() -> service.accepter("loc@test.fr", 50L)).extracting("status").isEqualTo(HttpStatus.CONFLICT);

        verify(bienRepository, never()).save(any());
        verifyNoInteractions(notificationService);
    }

    @Test
    @DisplayName("refuser : statut REFUSEE, bien non assigné, propriétaire prévenu")
    void refuser() {
        DemandeLocation d = demande(StatutDemandeLocation.EN_ATTENTE, MAINTENANT.plusHours(5));
        when(demandeRepository.findAvecParties(50L)).thenReturn(Optional.of(d));

        assertThat(service.refuser("loc@test.fr", 50L).statut()).isEqualTo(StatutDemandeLocation.REFUSEE);

        assertThat(bien.getLocataire()).isNull();
        verify(bienRepository, never()).save(any());
        verify(notificationService).saveAndSend(eq(proprietaire), eq(NotificationType.DEMANDE_LOCATION_REFUSEE),
                anyString(), anyString(), isNull(), eq(10L));
    }

    // ── Retrait par le propriétaire ──────────────────────────────────────────

    @Test
    @DisplayName("annuler : demande en attente retirée, locataire prévenu ; autre propriétaire → 403 ; déjà expirée → 409")
    void annuler() {
        DemandeLocation d = demande(StatutDemandeLocation.EN_ATTENTE, MAINTENANT.plusHours(5));
        when(demandeRepository.findAvecParties(50L)).thenReturn(Optional.of(d));

        assertThatThrownBy(() -> service.annuler("intrus@test.fr", 50L)).extracting("status").isEqualTo(HttpStatus.FORBIDDEN);

        service.annuler("proprio@test.fr", 50L);
        assertThat(d.getStatut()).isEqualTo(StatutDemandeLocation.ANNULEE);
        verify(notificationService).saveAndSend(eq(locataire), eq(NotificationType.DEMANDE_LOCATION_ANNULEE),
                anyString(), anyString(), isNull(), eq(50L));

        when(demandeRepository.findAvecParties(51L))
                .thenReturn(Optional.of(demande(StatutDemandeLocation.EN_ATTENTE, MAINTENANT.minusMinutes(1))));
        assertThatThrownBy(() -> service.annuler("proprio@test.fr", 51L)).extracting("status").isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    @DisplayName("statut affiché : une demande en attente dont le délai est passé est EXPIREE")
    void statutAffiche_expiree() {
        when(userService.getUserByEmail("proprio@test.fr")).thenReturn(proprietaire);
        when(bienService.verifierProprietaire(10L, "proprio@test.fr")).thenReturn(bien);
        when(demandeRepository.findDuBien(10L)).thenReturn(java.util.List.of(
                demande(StatutDemandeLocation.EN_ATTENTE, MAINTENANT.minusSeconds(1)),
                demande(StatutDemandeLocation.EN_ATTENTE, MAINTENANT.plusHours(1))));

        assertThat(service.duBien("proprio@test.fr", 10L)).extracting(DemandeLocationDTO::statut)
                .containsExactly(StatutDemandeLocation.EXPIREE, StatutDemandeLocation.EN_ATTENTE);
    }
}
