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

    /**
     * Retourne l'historique chronologique des messages entre deux utilisateurs pour un bien donné.
     *
     * @param bienId             identifiant du bien concerné
     * @param emailConnecte      email de l'utilisateur connecté
     * @param emailInterlocuteur email de l'interlocuteur
     * @return liste des messages triés du plus ancien au plus récent
     */
    List<MessageDTO> getHistorique(Long bienId, String emailConnecte, String emailInterlocuteur);

    /**
     * Retourne le nombre total de messages non lus pour l'utilisateur connecté.
     *
     * @param email email de l'utilisateur connecté
     * @return nombre de messages non lus toutes conversations confondues
     */
    Long countMessagesNonLus(String email);

    /**
     * Marque comme lus tous les messages reçus d'un expéditeur dans une conversation.
     *
     * @param emailDestinataire email du destinataire (utilisateur connecté)
     * @param emailExpediteur   email de l'expéditeur dont on marque les messages comme lus
     */
    void marquerConversationLue(String emailDestinataire, String emailExpediteur);

}
