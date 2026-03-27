package com.kupanga.api.chat.service;


import com.kupanga.api.chat.entity.Conversation;

public interface ConversationService {

    /**
     * Créer une nouvelle conversation
     * @param bienId id du bien concerné.
     * @param email email de l'expéditeur
     * @return Conversation.
     */
    Conversation createConversation(Long bienId ,String email );

    /**
     * Rétourne une conversation à partir de l'émail de l'utilisateur et id du bien
     * @param bienId id du bien
     * @param email email de l'expéditeur
     * @return Conversation
     */
    Conversation findConversationWithBienIdAndEmailExpediteur( Long bienId , String email);
}
