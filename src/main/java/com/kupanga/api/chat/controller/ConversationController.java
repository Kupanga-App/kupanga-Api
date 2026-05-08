package com.kupanga.api.chat.controller;

import com.kupanga.api.chat.dto.ConversationPageDTO;
import com.kupanga.api.chat.dto.ConversationSearchDTO;
import com.kupanga.api.chat.research.ConversationSearchService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/conversations")
@Tag(name = "Conversation" , description = " gestion des conversations au niveau du chat")
public class ConversationController {

    private final ConversationSearchService conversationSearchService;

    @PostMapping("/search")
    public ResponseEntity<ConversationPageDTO> rechercherConversations(
            @RequestBody(required = false) ConversationSearchDTO dto
    ) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (dto == null) dto = new ConversationSearchDTO(
                null, null, null, null, null, null
        );

        return ResponseEntity.ok(conversationSearchService.rechercher(auth.getName(), dto));
    }
}
