package com.kupanga.api.chat.dto;

import java.time.LocalDateTime;

public record ConversationDTO(
        Long id,
        Long bienId,
        String bienTitre,
        String emailExpediteur,
        String emailDestinataire,
        String expediteurName,
        String destinataireName,
        String expediteurPhotoUrl,
        String destinatairePhotoUrl,
        String lastMessage,
        LocalDateTime lastMessageAt,
        LocalDateTime createdAt,
        long nonLuCount
) {}
