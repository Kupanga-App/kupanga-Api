package com.kupanga.api.chat.service.impl;

import com.kupanga.api.chat.dto.MessageDTO;
import com.kupanga.api.chat.dto.MessagePayload;
import com.kupanga.api.chat.dto.NotificationDTO;
import com.kupanga.api.chat.entity.Conversation;
import com.kupanga.api.chat.entity.Message;
import com.kupanga.api.chat.mapper.MessageMapper;
import com.kupanga.api.chat.repository.MessageRepository;
import com.kupanga.api.chat.service.ConversationService;
import com.kupanga.api.chat.service.MessageService;
import com.kupanga.api.exception.business.KupangaBusinessException;
import com.kupanga.api.immobilier.entity.Bien;
import com.kupanga.api.immobilier.repository.BienRepository;
import com.kupanga.api.immobilier.service.BienService;
import com.kupanga.api.user.entity.User;
import com.kupanga.api.user.service.UserService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class MessageServiceImpl implements MessageService {

    private final MessageRepository messageRepository;
    private final MessageMapper messageMapper;
    private final UserService   userService;
    private final ConversationService conversationService;
    private final SimpMessagingTemplate messagingTemplate;  // pour le push WebSocket

    // ─────────────────────────────────────────────────────────────────────────
    // Envoi d'un message
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void envoyerMessage(MessagePayload payload, String emailExpediteur) {

        User expediteur   = userService.getUserByEmail(emailExpediteur);
        User destinataire = userService.getUserByEmail(payload.emailDestinataire());

        if (expediteur.getMail().equals(destinataire.getMail())) {
            throw new KupangaBusinessException(
                    "Impossible d'envoyer un message à soi-même", HttpStatus.BAD_REQUEST);
        }

        Conversation conversation = conversationService.findConversationWithBienIdAndEmailExpediteur(payload.bienId(), emailExpediteur , payload.emailDestinataire() );
        if( conversation == null){

            conversation = conversationService.createConversation(payload.bienId(),  emailExpediteur , payload.emailDestinataire());

        }

        conversation.setLastMessage(payload.contenu());
        conversation.setLastMessageAt(LocalDateTime.now());
        conversationService.save(conversation);
        // Persister le message
        Message message = Message.builder()
                .contenu(payload.contenu())
                .expediteur(expediteur)
                .destinataire(destinataire)
                .conversation(conversation)
                .build();

        Message saved = messageRepository.save(message);
        MessageDTO dto = messageMapper.toDTO(saved);

        // ─── Push WebSocket au destinataire ───────────────────────────────────
        // Envoie dans la queue privée du destinataire : /user/{email}/queue/messages
        try {
            log.info("[WS-PUSH] → Envoi vers /user/{}/queue/messages  |  msgId={} | de={} | contenu=\"{}\"",
                    destinataire.getMail(), saved.getId(), emailExpediteur,
                    payload.contenu().substring(0, Math.min(50, payload.contenu().length())));

            messagingTemplate.convertAndSendToUser(
                    destinataire.getMail(),
                    "/queue/messages",
                    dto
            );

            log.info("[WS-PUSH] ✓ Message poussé avec succès vers {}", destinataire.getMail());

        } catch (Exception e) {
            log.error("[WS-PUSH] ✗ Échec du push WebSocket vers {} : {}", destinataire.getMail(), e.getMessage(), e);
        }

        // ─── Push WebSocket notification ──────────────────────────────────────
        // Envoie sur /user/{email}/queue/notifications pour les vues hors-conversation
        try {
            NotificationDTO notif = new NotificationDTO(
                    conversation.getId(),
                    expediteur.getMail(),
                    expediteur.getFirstName() + " " + expediteur.getLastName(),
                    payload.contenu().substring(0, Math.min(60, payload.contenu().length())),
                    saved.getCreatedAt()
            );

            messagingTemplate.convertAndSendToUser(
                    destinataire.getMail(),
                    "/queue/notifications",
                    notif
            );

            log.info("[WS-NOTIF] ✓ Notification poussée vers {}", destinataire.getMail());

        } catch (Exception e) {
            log.error("[WS-NOTIF] ✗ Échec notification vers {} : {}", destinataire.getMail(), e.getMessage());
        }

        log.info("Message {} envoyé de {} à {}",
                saved.getId(), emailExpediteur, payload.emailDestinataire());

    }

    @Override
    public List<MessageDTO> getHistorique(Long bienId, String emailConnecte, String emailInterlocuteur) {
        return messageRepository
                .findHistorique(bienId, emailConnecte, emailInterlocuteur)
                .stream()
                .map(messageMapper::toDTO)
                .toList();
    }

    @Override
    public Long countMessagesNonLus(String email) {
        return messageRepository.countMessagesNonLus(email);
    }

    @Override
    public void marquerConversationLue(String emailDestinataire, String emailExpediteur) {
        messageRepository.marquerConversationLue(emailDestinataire, emailExpediteur);
    }
}
