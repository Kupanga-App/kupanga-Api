package com.kupanga.api.chat.service.impl;

import com.kupanga.api.chat.dto.MessageDTO;
import com.kupanga.api.chat.dto.MessagePayload;
import com.kupanga.api.chat.entity.Message;
import com.kupanga.api.chat.mapper.MessageMapper;
import com.kupanga.api.chat.repository.MessageRepository;
import com.kupanga.api.chat.service.MessageService;
import com.kupanga.api.exception.business.KupangaBusinessException;
import com.kupanga.api.immobilier.entity.Bien;
import com.kupanga.api.immobilier.repository.BienRepository;
import com.kupanga.api.user.entity.User;
import com.kupanga.api.user.service.UserService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class MessageServiceImpl implements MessageService {

    private final MessageRepository messageRepository;
    private final MessageMapper messageMapper;
    private final UserService          userService;
    private final BienRepository       bienRepository;
    private final SimpMessagingTemplate messagingTemplate;  // pour le push WebSocket

    // ─────────────────────────────────────────────────────────────────────────
    // Envoi d'un message
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void envoyerMessage(MessagePayload payload, String emailExpediteur) {

        User expediteur   = userService.getUserByEmail(emailExpediteur);
        User destinataire = userService.getUserByEmail(payload.getEmailDestinataire());

        if (expediteur.getMail().equals(destinataire.getMail())) {
            throw new KupangaBusinessException(
                    "Impossible d'envoyer un message à soi-même", HttpStatus.BAD_REQUEST);
        }

        // Bien optionnel
        Bien bien = null;
        if (payload.getBienId() != null) {
            bien = bienRepository.findById(payload.getBienId())
                    .orElseThrow(() -> new KupangaBusinessException(
                            "Bien introuvable : " + payload.getBienId(), HttpStatus.NOT_FOUND));
        }

        // Persister le message
        Message message = Message.builder()
                .contenu(payload.getContenu())
                .expediteur(expediteur)
                .destinataire(destinataire)
                .bien(bien)
                .lu(false)
                .build();

        Message saved = messageRepository.save(message);
        MessageDTO dto = messageMapper.toDTO(saved);

        // ─── Push WebSocket au destinataire ───────────────────────────────────
        // Envoie dans la queue privée du destinataire : /user/{email}/queue/messages
        messagingTemplate.convertAndSendToUser(
                destinataire.getMail(),
                "/queue/messages",
                dto
        );

        log.info("Message {} envoyé de {} à {}",
                saved.getId(), emailExpediteur, payload.getEmailDestinataire());

    }

    // ─────────────────────────────────────────────────────────────────────────
    // Lecture de conversation
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public List<MessageDTO> getConversation(String emailA, String emailB) {
        return messageRepository.findConversation(emailA, emailB)
                .stream()
                .map(messageMapper::toDTO)
                .toList();
    }

    @Override
    public List<MessageDTO> getConversationParBien(String emailA, String emailB, Long bienId) {
        return messageRepository.findConversationParBien(emailA, emailB, bienId)
                .stream()
                .map(messageMapper::toDTO)
                .toList();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Marquer comme lu
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void marquerConversationLue(String emailDestinataire, String emailExpediteur) {
        messageRepository.marquerConversationLue(emailDestinataire, emailExpediteur);
        log.info("Conversation entre {} et {} marquée comme lue", emailDestinataire, emailExpediteur);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Compteur non lus
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public Long countMessagesNonLus(String email) {
        return messageRepository.countMessagesNonLus(email);
    }
}
