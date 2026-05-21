package com.kupanga.api.notification.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.kupanga.api.notification.enums.NotificationType;

import java.time.LocalDateTime;

public record AppNotificationDTO(
        Long id,
        NotificationType type,
        String titre,
        String message,
        boolean lue,
        String lien,
        Long referenceId,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime createdAt
) {}
