package com.kupanga.api.chat.service;



import com.kupanga.api.chat.dto.MessageDTO;
import com.kupanga.api.chat.dto.MessagePayload;

import java.util.List;

public interface MessageService {

    /**
     * Envoie un message, le persiste en base et le retourne formaté.
     *
     * @param payload         contenu + destinataire + bienId optionnel
     * @param emailExpediteur email de l'expéditeur (extrait du JWT)
     */
    void envoyerMessage(MessagePayload payload, String emailExpediteur);

}
