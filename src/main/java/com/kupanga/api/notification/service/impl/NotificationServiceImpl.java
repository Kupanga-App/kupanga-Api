package com.kupanga.api.notification.service.impl;

import com.kupanga.api.exception.business.KupangaBusinessException;
import com.kupanga.api.notification.dto.AppNotificationDTO;
import com.kupanga.api.notification.entity.Notification;
import com.kupanga.api.notification.enums.NotificationType;
import com.kupanga.api.notification.repository.NotificationRepository;
import com.kupanga.api.notification.service.NotificationService;
import com.kupanga.api.user.entity.User;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate  messagingTemplate;

    @Override
    @Transactional
    public void saveAndSend(User destinataire, NotificationType type,
                            String titre, String message,
                            String lien, Long referenceId) {

        Notification notif = Notification.builder()
                .destinataire(destinataire)
                .type(type)
                .titre(titre)
                .message(message)
                .lien(lien)
                .referenceId(referenceId)
                .build();

        Notification saved = notificationRepository.save(notif);

        try {
            messagingTemplate.convertAndSendToUser(
                    destinataire.getMail(),
                    "/queue/app-notifications",
                    toDTO(saved)
            );
            log.info("[NOTIF] ✓ {} → {}", type, destinataire.getMail());
        } catch (Exception e) {
            log.error("[NOTIF] ✗ Push WebSocket échoué vers {} : {}", destinataire.getMail(), e.getMessage());
        }
    }

    @Override
    public List<AppNotificationDTO> getNonLues(String email) {
        return notificationRepository.findNonLuesByEmail(email)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    @Override
    @Transactional
    public void marquerLue(Long notifId, String email) {
        Notification notif = notificationRepository
                .findByIdAndDestinataireMail(notifId, email)
                .orElseThrow(() -> new KupangaBusinessException(
                        "Notification introuvable", HttpStatus.NOT_FOUND));
        notif.setLue(true);
        notificationRepository.save(notif);
    }

    @Override
    @Transactional
    public void marquerToutesLues(String email) {
        notificationRepository.marquerToutesLues(email);
    }

    private AppNotificationDTO toDTO(Notification n) {
        return new AppNotificationDTO(
                n.getId(),
                n.getType(),
                n.getTitre(),
                n.getMessage(),
                n.isLue(),
                n.getLien(),
                n.getReferenceId(),
                n.getCreatedAt()
        );
    }
}
