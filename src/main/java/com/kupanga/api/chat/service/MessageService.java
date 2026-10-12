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
    default void envoyerMessage(MessagePayload payload, String emailExpediteur) {
        envoyerMessage(payload, emailExpediteur, null);
    }

    /**
     * Comme {@link #envoyerMessage(MessagePayload, String)}, puis renvoie le message enregistré à l'expéditeur
     * sur {@code /user/queue/messages} avec {@code idClient} : accusé d'envoi du front (W13).
     *
     * @param idClient identifiant choisi par le front, déjà validé ; {@code null} = pas d'écho
     */
    void envoyerMessage(MessagePayload payload, String emailExpediteur, String idClient);

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
     * Marque comme lus les messages reçus d'un interlocuteur dans la conversation sur ce bien (W6).
     *
     * @param bienId            bien de la conversation
     * @param emailDestinataire email du destinataire (utilisateur connecté)
     * @param emailExpediteur   email de l'interlocuteur dont on marque les messages comme lus
     */
    void marquerConversationLue(Long bienId, String emailDestinataire, String emailExpediteur);

}
