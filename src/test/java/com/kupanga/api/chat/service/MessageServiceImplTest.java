package com.kupanga.api.chat.service;

import com.kupanga.api.chat.dto.MessageDTO;
import com.kupanga.api.chat.dto.MessagePayload;
import com.kupanga.api.chat.entity.Conversation;
import com.kupanga.api.chat.entity.Message;
import com.kupanga.api.chat.mapper.MessageMapper;
import com.kupanga.api.chat.repository.MessageRepository;
import com.kupanga.api.chat.service.impl.MessageServiceImpl;
import com.kupanga.api.exception.business.KupangaBusinessException;
import com.kupanga.api.user.entity.User;
import com.kupanga.api.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("Tests unitaires — MessageServiceImpl")
class MessageServiceImplTest {

    @Mock private MessageRepository      messageRepository;
    @Mock private MessageMapper          messageMapper;
    @Mock private UserService            userService;
    @Mock private ConversationService    conversationService;
    @Mock private SimpMessagingTemplate  messagingTemplate;

    @InjectMocks
    private MessageServiceImpl messageService;

    private User expediteur;
    private User destinataire;
    private Conversation conversation;
    private Message savedMessage;
    private MessageDTO messageDTO;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        expediteur = User.builder()
                .id(1L)
                .mail("alice@test.com")
                .firstName("Alice")
                .lastName("Martin")
                .build();

        destinataire = User.builder()
                .id(2L)
                .mail("bob@test.com")
                .firstName("Bob")
                .lastName("Dupont")
                .build();

        conversation = Conversation.builder()
                .id(10L)
                .emailExpediteur("alice@test.com")
                .emailDestinataire("bob@test.com")
                .build();

        savedMessage = Message.builder()
                .id(100L)
                .contenu("Bonjour")
                .expediteur(expediteur)
                .destinataire(destinataire)
                .conversation(conversation)
                .build();

        messageDTO = mock(MessageDTO.class);
    }

    // ══════════════════════════════════════════════════════════════
    // envoyerMessage
    // ══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("envoyerMessage() — conversation existante : message envoyé + WS poussé")
    void envoyerMessage_existingConversation_sendsAndPushes() {
        MessagePayload payload = new MessagePayload("Bonjour", "bob@test.com", 1L);

        when(userService.getUserByEmail("alice@test.com")).thenReturn(expediteur);
        when(userService.getUserByEmail("bob@test.com")).thenReturn(destinataire);
        when(conversationService.findConversationWithBienIdAndEmailExpediteur(1L, "alice@test.com", "bob@test.com"))
                .thenReturn(conversation);
        doNothing().when(conversationService).save(conversation);
        when(messageRepository.save(any(Message.class))).thenReturn(savedMessage);
        when(messageMapper.toDTO(savedMessage)).thenReturn(messageDTO);

        assertDoesNotThrow(() -> messageService.envoyerMessage(payload, "alice@test.com"));

        verify(messageRepository).save(any(Message.class));
        verify(messagingTemplate).convertAndSendToUser(eq("bob@test.com"), eq("/queue/messages"), eq(messageDTO));
        verify(conversationService, never()).createConversation(anyLong(), anyString(), anyString());
    }

    @Test
    @DisplayName("envoyerMessage() — pas de conversation : nouvelle conversation créée")
    void envoyerMessage_noConversation_createsNew() {
        MessagePayload payload = new MessagePayload("Bonjour", "bob@test.com", 1L);

        when(userService.getUserByEmail("alice@test.com")).thenReturn(expediteur);
        when(userService.getUserByEmail("bob@test.com")).thenReturn(destinataire);
        when(conversationService.findConversationWithBienIdAndEmailExpediteur(1L, "alice@test.com", "bob@test.com"))
                .thenReturn(null);
        when(conversationService.createConversation(1L, "alice@test.com", "bob@test.com"))
                .thenReturn(conversation);
        doNothing().when(conversationService).save(conversation);
        when(messageRepository.save(any(Message.class))).thenReturn(savedMessage);
        when(messageMapper.toDTO(savedMessage)).thenReturn(messageDTO);

        assertDoesNotThrow(() -> messageService.envoyerMessage(payload, "alice@test.com"));

        verify(conversationService).createConversation(1L, "alice@test.com", "bob@test.com");
    }

    @Test
    @DisplayName("envoyerMessage() — envoi à soi-même → KupangaBusinessException 400")
    void envoyerMessage_selfMessage_throwsBadRequest() {
        MessagePayload payload = new MessagePayload("Bonjour", "alice@test.com", 1L);

        when(userService.getUserByEmail("alice@test.com")).thenReturn(expediteur);

        KupangaBusinessException ex = assertThrows(KupangaBusinessException.class,
                () -> messageService.envoyerMessage(payload, "alice@test.com"));

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(messageRepository, never()).save(any());
    }

    // ══════════════════════════════════════════════════════════════
    // getHistorique
    // ══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("getHistorique() — retourne la liste des messages mappés")
    void getHistorique_returnsMappedList() {
        when(messageRepository.findHistorique(1L, "alice@test.com", "bob@test.com"))
                .thenReturn(List.of(savedMessage));
        when(messageMapper.toDTO(savedMessage)).thenReturn(messageDTO);

        List<MessageDTO> result = messageService.getHistorique(1L, "alice@test.com", "bob@test.com");

        assertThat(result).hasSize(1);
    }

    // ══════════════════════════════════════════════════════════════
    // countMessagesNonLus
    // ══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("countMessagesNonLus() — délègue au repository")
    void countMessagesNonLus_delegatesToRepository() {
        when(messageRepository.countMessagesNonLus("alice@test.com")).thenReturn(5L);

        Long result = messageService.countMessagesNonLus("alice@test.com");

        assertThat(result).isEqualTo(5L);
    }

    // ══════════════════════════════════════════════════════════════
    // marquerConversationLue
    // ══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("marquerConversationLue() — délègue au repository")
    void marquerConversationLue_delegatesToRepository() {
        doNothing().when(messageRepository).marquerConversationLue("alice@test.com", "bob@test.com");

        assertDoesNotThrow(() ->
                messageService.marquerConversationLue("alice@test.com", "bob@test.com"));

        verify(messageRepository).marquerConversationLue("alice@test.com", "bob@test.com");
    }
}
