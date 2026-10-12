package com.kupanga.api.chat.service.impl;

import com.kupanga.api.authentification.ratelimit.LimiteurTentatives;
import com.kupanga.api.chat.dto.MessageDTO;
import com.kupanga.api.chat.dto.MessagePayload;
import com.kupanga.api.config.ApresCommit;
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
import com.kupanga.api.user.utils.EmailUtils;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

import static com.kupanga.api.authentification.ratelimit.LimiteTentatives.NOUVELLE_CONVERSATION_PAR_HEURE;
import static com.kupanga.api.authentification.ratelimit.LimiteTentatives.NOUVELLE_CONVERSATION_PAR_JOUR;

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
    private final BienService bienService;
    private final LimiteurTentatives limiteurTentatives;

    // ─────────────────────────────────────────────────────────────────────────
    // Envoi d'un message
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void envoyerMessage(MessagePayload payload, String emailExpediteur, String idClient) {

        User expediteur   = userService.getUserByEmail(emailExpediteur);
        emailExpediteur   = expediteur.getMail();
        Bien bien         = bienService.findById(payload.bienId());
        User proprietaire = bien.getProprietaire();
        boolean expediteurEstProprietaire = proprietaire.getId().equals(expediteur.getId());

        // Sans e-mail (premier contact depuis une annonce publique, qui n'expose plus l'e-mail) :
        // le destinataire est le propriétaire du bien
        String emailDestinataire = EmailUtils.normaliser(payload.emailDestinataire());
        if (emailDestinataire == null || emailDestinataire.isBlank()) {
            emailDestinataire = proprietaire.getMail();
        }

        if (emailExpediteur.equals(emailDestinataire)) {
            throw new KupangaBusinessException(
                    "Impossible d'envoyer un message à soi-même", HttpStatus.BAD_REQUEST);
        }

        // W1 : l'un des deux participants est toujours le propriétaire du bien
        if (!expediteurEstProprietaire && !emailDestinataire.equals(proprietaire.getMail())) {
            throw conversationRefusee();
        }

        Conversation conversation = conversationService.findConversationWithBienIdAndEmailExpediteur(payload.bienId(), emailExpediteur , emailDestinataire );
        User destinataire;
        if (conversation != null) {
            destinataire = expediteurEstProprietaire ? userService.getUserByEmail(emailDestinataire) : proprietaire;
        } else {
            // W1 (décision 2026-10-09) : nouvelle conversation ouverte par un candidat sur un bien disponible
            // (ou par son locataire actuel), ou par le propriétaire avec son locataire actuel uniquement.
            // Le propriétaire ne peut donc plus créer seul la conversation « candidat » qui permet l'assignation.
            User locataire = bien.getLocataire();
            if (expediteurEstProprietaire) {
                if (locataire == null || !locataire.getMail().equals(emailDestinataire)) {
                    throw conversationRefusee();
                }
                destinataire = locataire;
            } else {
                if (locataire != null && !locataire.getId().equals(expediteur.getId())) {
                    throw new KupangaBusinessException("Ce bien n'est plus disponible", HttpStatus.FORBIDDEN);
                }
                destinataire = proprietaire;
            }
        }
        // B12 : plus d'envoi vers un compte anonymisé (personne ne lirait le message) ; bien archivé en lecture
        // seule (contrôlé après W1 : seul un participant légitime apprend que le bien est archivé)
        if (destinataire.isAnonymise()) {
            throw conversationRefusee();
        }
        bienService.verifierBienActif(bien);
        if (conversation == null) {
            // Le premier contact révèle l'e-mail du propriétaire : nombre de nouvelles conversations plafonné (429)
            limiteurTentatives.verifierEmail(NOUVELLE_CONVERSATION_PAR_HEURE, emailExpediteur);
            limiteurTentatives.verifierEmail(NOUVELLE_CONVERSATION_PAR_JOUR, emailExpediteur);
            conversation = conversationService.createConversation(payload.bienId(),  emailExpediteur , emailDestinataire);
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

        // B11 : envois WebSocket après le commit (pas de message « fantôme » si la transaction est annulée)
        ApresCommit.executer(envoisWebSocket(expediteur, destinataire, saved, dto,
                conversation.getId(), payload.contenu(), idClient));
    }

    private Runnable envoisWebSocket(User expediteur, User destinataire, Message saved, MessageDTO dto,
                                     Long conversationId, String contenu, String idClient) {
        return () -> {
            // ─── Push WebSocket au destinataire ───────────────────────────────────
            // Envoie dans la queue privée du destinataire : /user/{email}/queue/messages
            try {
                messagingTemplate.convertAndSendToUser(
                        destinataire.getMail(),
                        "/queue/messages",
                        dto
                );

                log.debug("[WS-PUSH] Message {} poussé (conversation {})", saved.getId(), conversationId);

            } catch (Exception e) {
                // W10 : ni e-mail ni contenu (le message d'une MessagingException contient le message STOMP)
                log.warn("[WS-PUSH] Échec du push du message {} : {}", saved.getId(), e.getClass().getName());
            }

            // ─── Écho à l'expéditeur (W13) ────────────────────────────────────────
            // Accusé d'envoi : le front remplace son message « en attente » par celui-ci (même idClient).
            if (idClient != null) {
                try {
                    messagingTemplate.convertAndSendToUser(
                            expediteur.getMail(), "/queue/messages", dto.toBuilder().idClient(idClient).build());
                } catch (Exception e) {
                    log.warn("[WS-ECHO] Échec de l'accusé du message {} : {}", saved.getId(), e.getClass().getName());
                }
            }

            // ─── Push WebSocket notification ──────────────────────────────────────
            // Envoie sur /user/{email}/queue/notifications pour les vues hors-conversation
            try {
                NotificationDTO notif = new NotificationDTO(
                        conversationId,
                        expediteur.getMail(),
                        expediteur.getFirstName() + " " + expediteur.getLastName(),
                        contenu.substring(0, Math.min(60, contenu.length())),
                        saved.getCreatedAt()
                );

                messagingTemplate.convertAndSendToUser(
                        destinataire.getMail(),
                        "/queue/notifications",
                        notif
                );

                log.debug("[WS-NOTIF] Notification du message {} poussée", saved.getId());

            } catch (Exception e) {
                log.warn("[WS-NOTIF] Échec de la notification du message {} : {}", saved.getId(), e.getClass().getName());
            }
        };
    }

    /** Refus générique : ne révèle pas si l'e-mail visé correspond à un compte. */
    private static KupangaBusinessException conversationRefusee() {
        return new KupangaBusinessException(
                "Vous ne pouvez pas écrire à cet utilisateur au sujet de ce bien", HttpStatus.FORBIDDEN);
    }

    @Override
    public List<MessageDTO> getHistorique(Long bienId, String emailConnecte, String emailInterlocuteur) {
        return messageRepository
                .findHistorique(bienId, emailConnecte, EmailUtils.normaliser(emailInterlocuteur))
                .stream()
                .map(messageMapper::toDTO)
                .toList();
    }

    @Override
    public Long countMessagesNonLus(String email) {
        return messageRepository.countMessagesNonLus(email);
    }

    @Override
    public void marquerConversationLue(Long bienId, String emailDestinataire, String emailExpediteur) {
        String moi = EmailUtils.normaliser(emailDestinataire);
        // W6 : seule la conversation de ce bien entre ces deux personnes (l'utilisateur connecté en fait partie)
        Conversation conversation = conversationService.findConversationWithBienIdAndEmailExpediteur(
                bienId, moi, EmailUtils.normaliser(emailExpediteur));
        if (conversation == null) return; // rien à marquer (ex. conversation pas encore créée)
        messageRepository.marquerConversationLue(conversation.getId(), moi);
    }
}
