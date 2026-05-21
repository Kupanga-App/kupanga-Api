package com.kupanga.api.notification.service;

import com.kupanga.api.exception.business.KupangaBusinessException;
import com.kupanga.api.notification.dto.AppNotificationDTO;
import com.kupanga.api.notification.entity.Notification;
import com.kupanga.api.notification.enums.NotificationType;
import com.kupanga.api.notification.repository.NotificationRepository;
import com.kupanga.api.notification.service.impl.NotificationServiceImpl;
import com.kupanga.api.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("Tests unitaires — NotificationServiceImpl")
class NotificationServiceImplTest {

    @Mock private NotificationRepository notificationRepository;
    @Mock private SimpMessagingTemplate  messagingTemplate;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    private User destinataire;
    private Notification notification;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        destinataire = User.builder()
                .id(1L)
                .mail("locataire@test.com")
                .firstName("Jean")
                .lastName("Dupont")
                .build();

        notification = Notification.builder()
                .id(10L)
                .destinataire(destinataire)
                .type(NotificationType.INVITATION_SIGNATURE_CONTRAT)
                .titre("Vous êtes invité à signer un contrat")
                .message("Le propriétaire vous invite à signer le contrat.")
                .lue(false)
                .lien("token-abc")
                .referenceId(1L)
                .build();
        notification.setCreatedAt(LocalDateTime.now());
    }

    // ══════════════════════════════════════════════════════════════
    // saveAndSend
    // ══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("saveAndSend() — notification persistée et push WebSocket envoyé")
    void saveAndSend_persistsAndPushesWebSocket() {
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);

        notificationService.saveAndSend(
                destinataire,
                NotificationType.INVITATION_SIGNATURE_CONTRAT,
                "Vous êtes invité à signer un contrat",
                "Le propriétaire vous invite à signer le contrat.",
                "token-abc",
                1L
        );

        verify(notificationRepository).save(any(Notification.class));
        verify(messagingTemplate).convertAndSendToUser(
                eq(destinataire.getMail()),
                eq("/queue/app-notifications"),
                any(AppNotificationDTO.class)
        );
    }

    @Test
    @DisplayName("saveAndSend() — échec WebSocket ne propage pas l'exception (notification toujours sauvée)")
    void saveAndSend_webSocketFailure_doesNotThrow() {
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);
        doThrow(new RuntimeException("WS error")).when(messagingTemplate)
                .convertAndSendToUser(anyString(), anyString(), any());

        assertDoesNotThrow(() -> notificationService.saveAndSend(
                destinataire,
                NotificationType.CONTRAT_SIGNE,
                "Contrat signé",
                "Votre contrat a été signé.",
                null,
                1L
        ));

        verify(notificationRepository).save(any(Notification.class));
    }

    // ══════════════════════════════════════════════════════════════
    // getNonLues
    // ══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("getNonLues() — retourne la liste des DTOs non lus")
    void getNonLues_returnsListOfDTO() {
        when(notificationRepository.findNonLuesByEmail(destinataire.getMail()))
                .thenReturn(List.of(notification));

        List<AppNotificationDTO> result = notificationService.getNonLues(destinataire.getMail());

        assertThat(result).hasSize(1);
        AppNotificationDTO dto = result.get(0);
        assertThat(dto.id()).isEqualTo(10L);
        assertThat(dto.type()).isEqualTo(NotificationType.INVITATION_SIGNATURE_CONTRAT);
        assertThat(dto.lue()).isFalse();
        assertThat(dto.lien()).isEqualTo("token-abc");
    }

    @Test
    @DisplayName("getNonLues() — aucune notification → liste vide")
    void getNonLues_empty_returnsEmptyList() {
        when(notificationRepository.findNonLuesByEmail(destinataire.getMail()))
                .thenReturn(List.of());

        List<AppNotificationDTO> result = notificationService.getNonLues(destinataire.getMail());

        assertThat(result).isEmpty();
    }

    // ══════════════════════════════════════════════════════════════
    // marquerLue
    // ══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("marquerLue() — succès : notification marquée lue et sauvegardée")
    void marquerLue_success_setsLueTrue() {
        when(notificationRepository.findByIdAndDestinataireMail(10L, destinataire.getMail()))
                .thenReturn(Optional.of(notification));
        when(notificationRepository.save(notification)).thenReturn(notification);

        notificationService.marquerLue(10L, destinataire.getMail());

        assertThat(notification.isLue()).isTrue();
        verify(notificationRepository).save(notification);
    }

    @Test
    @DisplayName("marquerLue() — notification introuvable → KupangaBusinessException 404")
    void marquerLue_notFound_throwsException() {
        when(notificationRepository.findByIdAndDestinataireMail(99L, destinataire.getMail()))
                .thenReturn(Optional.empty());

        KupangaBusinessException ex = assertThrows(KupangaBusinessException.class,
                () -> notificationService.marquerLue(99L, destinataire.getMail()));

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        verify(notificationRepository, never()).save(any());
    }

    // ══════════════════════════════════════════════════════════════
    // marquerToutesLues
    // ══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("marquerToutesLues() — délègue au repository")
    void marquerToutesLues_callsRepository() {
        notificationService.marquerToutesLues(destinataire.getMail());

        verify(notificationRepository).marquerToutesLues(destinataire.getMail());
    }
}
