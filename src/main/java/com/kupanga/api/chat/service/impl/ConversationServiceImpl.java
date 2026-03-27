package com.kupanga.api.chat.service.impl;

import com.kupanga.api.chat.entity.Conversation;
import com.kupanga.api.chat.repository.ConversationRepository;
import com.kupanga.api.chat.service.ConversationService;
import com.kupanga.api.immobilier.entity.Bien;
import com.kupanga.api.immobilier.service.BienService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ConversationServiceImpl implements ConversationService {

    private final BienService bienService;
    private final ConversationRepository conversationRepository;

    @Override
    public Conversation createConversation(Long bienId , String email) {

        Bien bien = bienService.findById(bienId);

        return conversationRepository.save(

                Conversation.builder()
                        .bien(bien)
                        .emailExpediteur(email)
                        .build()
        );
    }

    @Override
    public Conversation findConversationWithBienIdAndEmailExpediteur(Long bienId, String email) {

        return conversationRepository.findConversationWithBienIdAndEmailExpediteur(bienId , email)
                .orElse(null) ;
    }
}
