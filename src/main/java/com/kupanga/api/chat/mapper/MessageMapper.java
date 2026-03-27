package com.kupanga.api.chat.mapper;


import com.kupanga.api.chat.dto.MessageDTO;
import com.kupanga.api.chat.entity.Message;
import org.springframework.stereotype.Component;

@Component
public class MessageMapper {

    public MessageDTO toDTO(Message message) {
        return MessageDTO.builder()
                .id(message.getId())
                .contenu(message.getContenu())
                .lu(message.getLu())
                .createdAt(message.getCreatedAt())
                // Expéditeur
                .expediteurId(message.getExpediteur().getId())
                .expediteurNom(message.getExpediteur().getFirstName()
                        + " " + message.getExpediteur().getLastName())
                .expediteurEmail(message.getExpediteur().getMail())
                // Destinataire
                .destinataireId(message.getDestinataire().getId())
                .destinataireNom(message.getDestinataire().getFirstName()
                        + " " + message.getDestinataire().getLastName())
                .destinataireEmail(message.getDestinataire().getMail())

                .build();
    }
}
