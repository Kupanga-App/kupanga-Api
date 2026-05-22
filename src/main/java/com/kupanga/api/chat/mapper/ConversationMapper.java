package com.kupanga.api.chat.mapper;

import com.kupanga.api.chat.dto.ConversationDTO;
import com.kupanga.api.chat.entity.Conversation;
import com.kupanga.api.user.entity.User;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface ConversationMapper {

    @Mapping(source = "conversation.id", target = "id")
    @Mapping(source = "conversation.bien.id", target = "bienId")
    @Mapping(source = "conversation.bien.titre", target = "bienTitre")
    @Mapping(source = "conversation.emailExpediteur", target = "emailExpediteur")
    @Mapping(source = "conversation.emailDestinataire", target = "emailDestinataire")
    @Mapping(source = "conversation.lastMessage", target = "lastMessage")
    @Mapping(source = "conversation.lastMessageAt", target = "lastMessageAt")
    @Mapping(source = "conversation.createdAt", target = "createdAt")
    @Mapping(expression = "java(expediteur.getFirstName() + ' ' + expediteur.getLastName())", target = "expediteurName")
    @Mapping(expression = "java(destinataire.getFirstName() + ' ' + destinataire.getLastName())", target = "destinataireName")
    @Mapping(source = "expediteur.urlProfile", target = "expediteurPhotoUrl")
    @Mapping(source = "destinataire.urlProfile", target = "destinatairePhotoUrl")
    @Mapping(target = "nonLuCount", constant = "0l")
    ConversationDTO toDto(Conversation conversation, User expediteur, User destinataire);
}
