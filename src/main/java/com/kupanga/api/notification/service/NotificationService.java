package com.kupanga.api.notification.service;

import com.kupanga.api.notification.dto.AppNotificationDTO;
import com.kupanga.api.notification.enums.NotificationType;
import com.kupanga.api.user.entity.User;

import java.util.List;

public interface NotificationService {

    void saveAndSend(User destinataire, NotificationType type,
                     String titre, String message,
                     String lien, Long referenceId);

    List<AppNotificationDTO> getNonLues(String email);

    void marquerLue(Long notifId, String email);

    void marquerToutesLues(String email);
}
