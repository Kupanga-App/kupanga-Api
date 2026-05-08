package com.kupanga.api.chat.dto;

import java.time.LocalDateTime;

public record ConversationDTO(
        Long id,
        Long bienId,
        String bienTitre,
        String emailExpediteur,
        String emailDestinataire,
        String lastMessage,
        LocalDateTime lastMessageAt,
        LocalDateTime createdAt
) {}
