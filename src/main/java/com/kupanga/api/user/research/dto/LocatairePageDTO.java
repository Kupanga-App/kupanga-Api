package com.kupanga.api.user.research.dto;

import com.kupanga.api.user.dto.readDTO.UserDTO;
import org.springframework.data.domain.Page;

import java.util.List;

public record LocatairePageDTO(
        List<UserDTO> contenu,
        int           pageActuelle,
        int           totalPages,
        long          totalElements,
        boolean       dernierePage,
        boolean       premierePage
) {
    public static LocatairePageDTO from(Page<UserDTO> page) {
        return new LocatairePageDTO(
                page.getContent(),
                page.getNumber(),
                page.getTotalPages(),
                page.getTotalElements(),
                page.isLast(),
                page.isFirst()
        );
    }
}