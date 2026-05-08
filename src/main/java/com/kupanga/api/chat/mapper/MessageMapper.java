package com.kupanga.api.chat.mapper;

import com.kupanga.api.chat.dto.MessageDTO;
import com.kupanga.api.chat.entity.Message;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface MessageMapper {

    @Mapping(target = "expediteurId", source = "expediteur.id")
    @Mapping(target = "expediteurEmail", source = "expediteur.mail")
    @Mapping(
            target = "expediteurNom",
            expression = "java(message.getExpediteur().getFirstName() + \" \" + message.getExpediteur().getLastName())"
    )

    @Mapping(target = "destinataireId", source = "destinataire.id")
    @Mapping(target = "destinataireEmail", source = "destinataire.mail")
    @Mapping(
            target = "destinataireNom",
            expression = "java(message.getDestinataire().getFirstName() + \" \" + message.getDestinataire().getLastName())"
    )

    MessageDTO toDTO(Message message);
}