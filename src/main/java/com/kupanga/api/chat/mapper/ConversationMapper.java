package com.kupanga.api.chat.mapper;

import com.kupanga.api.chat.dto.ConversationDTO;
import com.kupanga.api.chat.entity.Conversation;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface ConversationMapper {

    @Mapping(source = "bien.id", target = "bienId")
    @Mapping(source = "bien.titre", target = "bienTitre")
    @Mapping(target = "nonLuCount", constant = "0l")
    ConversationDTO toDto(Conversation conversation);
}
