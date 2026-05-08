package com.kupanga.api.chat.dto;

import org.springframework.data.domain.Page;

import java.util.List;

public record ConversationPageDTO(

        List<ConversationDTO> contenu ,
        int           pageActuelle,
        int           totalPages,
        long          totalElements,
        boolean       dernierePage,
        boolean       premierePage
) {
    public static ConversationPageDTO from(Page<ConversationDTO> page) {
        return new ConversationPageDTO(
                page.getContent(),
                page.getNumber(),
                page.getTotalPages(),
                page.getTotalElements(),
                page.isLast(),
                page.isFirst()
        );
    }
}
