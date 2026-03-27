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
     * Récupère la conversation complète entre deux utilisateurs.
     *
     * @param emailA premier utilisateur
     * @param emailB second utilisateur
     * @return liste des messages triés par date croissante
     */
    List<MessageDTO> getConversation(String emailA, String emailB);

    /**
     * Récupère la conversation dans le contexte d'un bien.
     *
     * @param emailA premier utilisateur
     * @param emailB second utilisateur
     * @param bienId id du bien
     * @return liste des messages triés par date croissante
     */
    List<MessageDTO> getConversationParBien(String emailA, String emailB, Long bienId);

    /**
     * Marque tous les messages d'une conversation comme lus.
     *
     * @param emailDestinataire l'utilisateur connecté qui lit
     * @param emailExpediteur   l'expéditeur de la conversation
     */
    void marquerConversationLue(String emailDestinataire, String emailExpediteur);

    /**
     * Retourne le nombre de messages non lus pour l'utilisateur connecté.
     *
     * @param email email de l'utilisateur connecté
     * @return nombre de messages non lus
     */
    Long countMessagesNonLus(String email);
}
