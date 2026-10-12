package com.kupanga.api.chat.service;

import com.kupanga.api.juridiction.Pays;
import com.kupanga.api.authentification.ratelimit.LimiteTentatives;
import com.kupanga.api.authentification.ratelimit.LimiteurTentatives;
import com.kupanga.api.exception.business.TropDeTentativesException;

import com.kupanga.api.chat.dto.MessageDTO;
import com.kupanga.api.chat.dto.MessagePayload;
import com.kupanga.api.immobilier.entity.Bien;
import com.kupanga.api.immobilier.service.BienService;
import com.kupanga.api.chat.entity.Conversation;
import com.kupanga.api.chat.entity.Message;
import com.kupanga.api.chat.mapper.MessageMapper;
import com.kupanga.api.chat.repository.MessageRepository;
import com.kupanga.api.chat.service.impl.MessageServiceImpl;
import com.kupanga.api.exception.business.KupangaBusinessException;
import com.kupanga.api.user.entity.User;
import com.kupanga.api.user.service.UserService;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.MessageDeliveryException;
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
    @Mock private BienService            bienService;
    @Mock private LimiteurTentatives     limiteurTentatives;

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

    // Bien de Bob (propriétaire), disponible ; Alice est candidate, Carol un tiers
    private Bien bienDeBob(User locataire) {
        return Bien.builder().pays(Pays.FR).id(1L).proprietaire(destinataire).locataire(locataire).build();
    }

    private User carol() {
        return User.builder().id(3L).mail("carol@test.com").firstName("Carol").lastName("Petit").build();
    }

    private void envoiPossible() {
        when(messageRepository.save(any(Message.class))).thenReturn(savedMessage);
        when(messageMapper.toDTO(savedMessage)).thenReturn(messageDTO);
    }

    private void assertRefus(MessagePayload payload, String emailExpediteur, HttpStatus statut) {
        KupangaBusinessException ex = assertThrows(KupangaBusinessException.class,
                () -> messageService.envoyerMessage(payload, emailExpediteur));
        assertThat(ex.getStatus()).isEqualTo(statut);
        verify(messageRepository, never()).save(any());
        verify(conversationService, never()).createConversation(anyLong(), anyString(), anyString());
        verifyNoInteractions(messagingTemplate);
    }

    @Test
    @DisplayName("Premier contact : nouvelles conversations plafonnées par compte (heure et jour) ; au-delà, rien n'est créé")
    void envoyerMessage_nouvelleConversation_plafonnee() {
        MessagePayload payload = new MessagePayload("Bonjour", null, 1L);
        when(userService.getUserByEmail("alice@test.com")).thenReturn(expediteur);
        when(bienService.findById(1L)).thenReturn(bienDeBob(null));
        when(conversationService.findConversationWithBienIdAndEmailExpediteur(1L, "alice@test.com", "bob@test.com"))
                .thenReturn(null);
        doThrow(new TropDeTentativesException(60))
                .when(limiteurTentatives).verifierEmail(LimiteTentatives.NOUVELLE_CONVERSATION_PAR_JOUR, "alice@test.com");

        assertThrows(TropDeTentativesException.class, () -> messageService.envoyerMessage(payload, "alice@test.com"));

        verify(limiteurTentatives).verifierEmail(LimiteTentatives.NOUVELLE_CONVERSATION_PAR_HEURE, "alice@test.com");
        verify(conversationService, never()).createConversation(anyLong(), anyString(), anyString());
        verify(messageRepository, never()).save(any());
    }

    @Test
    @DisplayName("Conversation existante : aucun plafond de premier contact consommé")
    void envoyerMessage_conversationExistante_sansPlafond() {
        MessagePayload payload = new MessagePayload("Bonjour", "bob@test.com", 1L);
        when(userService.getUserByEmail("alice@test.com")).thenReturn(expediteur);
        when(bienService.findById(1L)).thenReturn(bienDeBob(null));
        when(conversationService.findConversationWithBienIdAndEmailExpediteur(1L, "alice@test.com", "bob@test.com"))
                .thenReturn(conversation);
        envoiPossible();

        messageService.envoyerMessage(payload, "alice@test.com");

        verifyNoInteractions(limiteurTentatives);
    }

    @Test
    @DisplayName("B12 : envoyerMessage() — bien archivé → 409, même dans une conversation existante")
    void envoyerMessage_bienArchive_refuse() {
        MessagePayload payload = new MessagePayload("Bonjour", "bob@test.com", 1L);
        Bien bien = bienDeBob(null);

        when(userService.getUserByEmail("alice@test.com")).thenReturn(expediteur);
        when(bienService.findById(1L)).thenReturn(bien);
        doThrow(new KupangaBusinessException("Ce bien est archivé", HttpStatus.CONFLICT))
                .when(bienService).verifierBienActif(bien);
        when(conversationService.findConversationWithBienIdAndEmailExpediteur(1L, "alice@test.com", "bob@test.com"))
                .thenReturn(conversation);

        assertRefus(payload, "alice@test.com", HttpStatus.CONFLICT);
    }

    @Test
    @DisplayName("B12 : envoyerMessage() — destinataire anonymisé (conversation existante) → 403, rien n'est enregistré")
    void envoyerMessage_destinataireAnonymise_refuse() {
        MessagePayload payload = new MessagePayload("Bonjour", "alice@test.com", 1L);
        expediteur.setAnonymise(true);

        when(userService.getUserByEmail("bob@test.com")).thenReturn(destinataire);
        when(userService.getUserByEmail("alice@test.com")).thenReturn(expediteur);
        when(bienService.findById(1L)).thenReturn(bienDeBob(null));
        when(conversationService.findConversationWithBienIdAndEmailExpediteur(1L, "bob@test.com", "alice@test.com"))
                .thenReturn(conversation);

        assertRefus(payload, "bob@test.com", HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("envoyerMessage() — conversation existante : message envoyé + WS poussé")
    void envoyerMessage_existingConversation_sendsAndPushes() {
        MessagePayload payload = new MessagePayload("Bonjour", "bob@test.com", 1L);

        when(userService.getUserByEmail("alice@test.com")).thenReturn(expediteur);
        when(bienService.findById(1L)).thenReturn(bienDeBob(null));
        when(conversationService.findConversationWithBienIdAndEmailExpediteur(1L, "alice@test.com", "bob@test.com"))
                .thenReturn(conversation);
        envoiPossible();

        assertDoesNotThrow(() -> messageService.envoyerMessage(payload, "alice@test.com"));

        verify(messageRepository).save(any(Message.class));
        verify(messagingTemplate).convertAndSendToUser(eq("bob@test.com"), eq("/queue/messages"), eq(messageDTO));
        verify(conversationService, never()).createConversation(anyLong(), anyString(), anyString());
    }

    @Test
    @DisplayName("envoyerMessage() — W13 : avec idClient, écho du message enregistré à l'expéditeur (le destinataire n'a pas l'idClient)")
    void envoyerMessage_avecIdClient_echoAExpediteur() {
        MessagePayload payload = new MessagePayload("Bonjour", "bob@test.com", 1L);
        MessageDTO dto = MessageDTO.builder().id(100L).contenu("Bonjour").build();

        when(userService.getUserByEmail("alice@test.com")).thenReturn(expediteur);
        when(bienService.findById(1L)).thenReturn(bienDeBob(null));
        when(conversationService.findConversationWithBienIdAndEmailExpediteur(1L, "alice@test.com", "bob@test.com"))
                .thenReturn(conversation);
        when(messageRepository.save(any(Message.class))).thenReturn(savedMessage);
        when(messageMapper.toDTO(savedMessage)).thenReturn(dto);

        messageService.envoyerMessage(payload, "alice@test.com", "abc-123");

        ArgumentCaptor<Object> versBob = ArgumentCaptor.forClass(Object.class);
        verify(messagingTemplate).convertAndSendToUser(eq("bob@test.com"), eq("/queue/messages"), versBob.capture());
        assertThat(((MessageDTO) versBob.getValue()).getIdClient()).isNull();

        ArgumentCaptor<Object> versAlice = ArgumentCaptor.forClass(Object.class);
        verify(messagingTemplate).convertAndSendToUser(eq("alice@test.com"), eq("/queue/messages"), versAlice.capture());
        MessageDTO echo = (MessageDTO) versAlice.getValue();
        assertThat(echo.getIdClient()).isEqualTo("abc-123");
        assertThat(echo.getId()).isEqualTo(100L);
    }

    @Test
    @DisplayName("envoyerMessage() — W13 : sans idClient, pas d'écho ; refus (W1) : ni enregistrement ni écho")
    void envoyerMessage_sansIdClientOuRefuse_pasDEcho() {
        MessagePayload payload = new MessagePayload("Bonjour", "bob@test.com", 1L);
        when(userService.getUserByEmail("alice@test.com")).thenReturn(expediteur);
        when(bienService.findById(1L)).thenReturn(bienDeBob(null));
        when(conversationService.findConversationWithBienIdAndEmailExpediteur(1L, "alice@test.com", "bob@test.com"))
                .thenReturn(conversation);
        envoiPossible();

        messageService.envoyerMessage(payload, "alice@test.com");

        verify(messagingTemplate, never()).convertAndSendToUser(eq("alice@test.com"), anyString(), any(Object.class));

        // Tiers sur un bien loué : refus avant tout envoi, même avec un idClient
        reset(messagingTemplate, messageRepository);
        when(userService.getUserByEmail("carol@test.com")).thenReturn(carol());
        when(bienService.findById(1L)).thenReturn(bienDeBob(expediteur));
        assertThrows(KupangaBusinessException.class,
                () -> messageService.envoyerMessage(payload, "carol@test.com", "abc-123"));
        verifyNoInteractions(messagingTemplate);
        verify(messageRepository, never()).save(any());
    }

    @Test
    @DisplayName("envoyerMessage() — W10 : ni contenu ni e-mail dans les logs, rien au-dessus de DEBUG (même en échec du push)")
    void envoyerMessage_logsSansContenuNiEmail() {
        String secret = "Mon code de la porte est 4721";
        MessagePayload payload = new MessagePayload(secret, "bob@test.com", 1L);
        savedMessage.setContenu(secret);

        when(userService.getUserByEmail("alice@test.com")).thenReturn(expediteur);
        when(bienService.findById(1L)).thenReturn(bienDeBob(null));
        when(conversationService.findConversationWithBienIdAndEmailExpediteur(1L, "alice@test.com", "bob@test.com"))
                .thenReturn(conversation);
        envoiPossible();

        Logger logger = (Logger) LoggerFactory.getLogger(MessageServiceImpl.class);
        Level niveauInitial = logger.getLevel();
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        logger.setLevel(Level.TRACE);
        try {
            // 1er envoi nominal, 2e envoi avec un push qui échoue (MessagingException porte le message STOMP)
            messageService.envoyerMessage(payload, "alice@test.com");
            doThrow(new MessageDeliveryException(
                    org.springframework.messaging.support.MessageBuilder.withPayload(secret).build(), "échec"))
                    .when(messagingTemplate).convertAndSendToUser(anyString(), anyString(), any(Object.class));
            messageService.envoyerMessage(payload, "alice@test.com");
        } finally {
            logger.detachAppender(appender);
            logger.setLevel(niveauInitial);
        }

        assertThat(appender.list).isNotEmpty();
        for (ILoggingEvent evenement : appender.list) {
            String texte = evenement.getFormattedMessage()
                    + (evenement.getThrowableProxy() != null ? evenement.getThrowableProxy().getMessage() : "");
            assertThat(texte).doesNotContain("4721", "alice@test.com", "bob@test.com");
        }
        // Nominal : rien en INFO ou au-dessus
        assertThat(appender.list.stream()
                .filter(e -> e.getLevel().isGreaterOrEqual(Level.INFO))
                .filter(e -> !e.getFormattedMessage().contains("Échec")))
                .isEmpty();
    }

    @Test
    @DisplayName("envoyerMessage() — sans e-mail destinataire : envoyé au propriétaire du bien (P0-6)")
    void envoyerMessage_sansEmail_envoieAuProprietaireDuBien() {
        MessagePayload payload = new MessagePayload("Bonjour", null, 1L);

        when(userService.getUserByEmail("alice@test.com")).thenReturn(expediteur);
        when(bienService.findById(1L)).thenReturn(bienDeBob(null));
        when(conversationService.findConversationWithBienIdAndEmailExpediteur(1L, "alice@test.com", "bob@test.com"))
                .thenReturn(conversation);
        envoiPossible();

        assertDoesNotThrow(() -> messageService.envoyerMessage(payload, "alice@test.com"));

        verify(userService, never()).getUserByEmail("bob@test.com");
        verify(messagingTemplate).convertAndSendToUser(eq("bob@test.com"), eq("/queue/messages"), eq(messageDTO));
    }

    @Test
    @DisplayName("envoyerMessage() — sans e-mail, le propriétaire s'écrit à lui-même → 400")
    void envoyerMessage_sansEmail_proprietaireLuiMeme_throwsBadRequest() {
        MessagePayload payload = new MessagePayload("Bonjour", null, 1L);
        Bien bien = Bien.builder().pays(Pays.FR).id(1L).proprietaire(expediteur).build();

        when(userService.getUserByEmail("alice@test.com")).thenReturn(expediteur);
        when(bienService.findById(1L)).thenReturn(bien);

        assertRefus(payload, "alice@test.com", HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("envoyerMessage() — bien inexistant → 404, rien enregistré")
    void envoyerMessage_sansEmail_bienInexistant_throwsNotFound() {
        MessagePayload payload = new MessagePayload("Bonjour", null, 99L);

        when(userService.getUserByEmail("alice@test.com")).thenReturn(expediteur);
        when(bienService.findById(99L))
                .thenThrow(new KupangaBusinessException("Aucun bien", HttpStatus.NOT_FOUND));

        assertRefus(payload, "alice@test.com", HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("envoyerMessage() — candidat, bien disponible, pas de conversation : nouvelle conversation créée")
    void envoyerMessage_noConversation_createsNew() {
        MessagePayload payload = new MessagePayload("Bonjour", "Bob@Test.com", 1L);

        when(userService.getUserByEmail("alice@test.com")).thenReturn(expediteur);
        when(bienService.findById(1L)).thenReturn(bienDeBob(null));
        when(conversationService.findConversationWithBienIdAndEmailExpediteur(1L, "alice@test.com", "bob@test.com"))
                .thenReturn(null);
        when(conversationService.createConversation(1L, "alice@test.com", "bob@test.com"))
                .thenReturn(conversation);
        envoiPossible();

        assertDoesNotThrow(() -> messageService.envoyerMessage(payload, "alice@test.com"));

        verify(conversationService).createConversation(1L, "alice@test.com", "bob@test.com");
        verify(messagingTemplate).convertAndSendToUser(eq("bob@test.com"), eq("/queue/messages"), eq(messageDTO));
    }

    @Test
    @DisplayName("envoyerMessage() — envoi à soi-même → KupangaBusinessException 400")
    void envoyerMessage_selfMessage_throwsBadRequest() {
        MessagePayload payload = new MessagePayload("Bonjour", "alice@test.com", 1L);

        when(userService.getUserByEmail("alice@test.com")).thenReturn(expediteur);
        when(bienService.findById(1L)).thenReturn(bienDeBob(null));

        assertRefus(payload, "alice@test.com", HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("W1 — un candidat écrit à un tiers (pas le propriétaire du bien) → 403, sans révéler si le compte existe")
    void envoyerMessage_candidatVersTiers_refuse() {
        MessagePayload payload = new MessagePayload("Bonjour", "carol@test.com", 1L);

        when(userService.getUserByEmail("alice@test.com")).thenReturn(expediteur);
        when(bienService.findById(1L)).thenReturn(bienDeBob(null));

        assertRefus(payload, "alice@test.com", HttpStatus.FORBIDDEN);
        verify(userService, never()).getUserByEmail("carol@test.com");
    }

    @Test
    @DisplayName("W1 — le propriétaire ouvre une conversation avec un inconnu → 403 (pas d'assignation sans accord)")
    void envoyerMessage_proprietaireVersInconnu_refuse() {
        MessagePayload payload = new MessagePayload("Bonjour", "carol@test.com", 1L);

        when(userService.getUserByEmail("bob@test.com")).thenReturn(destinataire);
        when(bienService.findById(1L)).thenReturn(bienDeBob(null));
        when(conversationService.findConversationWithBienIdAndEmailExpediteur(1L, "bob@test.com", "carol@test.com"))
                .thenReturn(null);

        assertRefus(payload, "bob@test.com", HttpStatus.FORBIDDEN);
        verify(userService, never()).getUserByEmail("carol@test.com");
    }

    @Test
    @DisplayName("W1 — le propriétaire ouvre une conversation avec son locataire actuel → créée")
    void envoyerMessage_proprietaireVersLocataireActuel_cree() {
        User carol = carol();
        MessagePayload payload = new MessagePayload("Bonjour", "carol@test.com", 1L);

        when(userService.getUserByEmail("bob@test.com")).thenReturn(destinataire);
        when(bienService.findById(1L)).thenReturn(bienDeBob(carol));
        when(conversationService.findConversationWithBienIdAndEmailExpediteur(1L, "bob@test.com", "carol@test.com"))
                .thenReturn(null);
        when(conversationService.createConversation(1L, "bob@test.com", "carol@test.com")).thenReturn(conversation);
        envoiPossible();

        assertDoesNotThrow(() -> messageService.envoyerMessage(payload, "bob@test.com"));

        verify(messagingTemplate).convertAndSendToUser(eq("carol@test.com"), eq("/queue/messages"), eq(messageDTO));
    }

    @Test
    @DisplayName("W1 — le propriétaire répond dans une conversation ouverte par un candidat → envoyé")
    void envoyerMessage_proprietaireRepondAuCandidat() {
        MessagePayload payload = new MessagePayload("Bonjour", "alice@test.com", 1L);

        when(userService.getUserByEmail("bob@test.com")).thenReturn(destinataire);
        when(userService.getUserByEmail("alice@test.com")).thenReturn(expediteur);
        when(bienService.findById(1L)).thenReturn(bienDeBob(null));
        when(conversationService.findConversationWithBienIdAndEmailExpediteur(1L, "bob@test.com", "alice@test.com"))
                .thenReturn(conversation);
        envoiPossible();

        assertDoesNotThrow(() -> messageService.envoyerMessage(payload, "bob@test.com"));

        verify(messagingTemplate).convertAndSendToUser(eq("alice@test.com"), eq("/queue/messages"), eq(messageDTO));
    }

    @Test
    @DisplayName("W1 — nouveau candidat sur un bien déjà loué → 403")
    void envoyerMessage_candidatBienLoue_refuse() {
        MessagePayload payload = new MessagePayload("Bonjour", null, 1L);

        when(userService.getUserByEmail("alice@test.com")).thenReturn(expediteur);
        when(bienService.findById(1L)).thenReturn(bienDeBob(carol()));
        when(conversationService.findConversationWithBienIdAndEmailExpediteur(1L, "alice@test.com", "bob@test.com"))
                .thenReturn(null);

        assertRefus(payload, "alice@test.com", HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("W1 — bien loué : une conversation déjà ouverte continue")
    void envoyerMessage_bienLoue_conversationExistante_continue() {
        MessagePayload payload = new MessagePayload("Bonjour", null, 1L);

        when(userService.getUserByEmail("alice@test.com")).thenReturn(expediteur);
        when(bienService.findById(1L)).thenReturn(bienDeBob(carol()));
        when(conversationService.findConversationWithBienIdAndEmailExpediteur(1L, "alice@test.com", "bob@test.com"))
                .thenReturn(conversation);
        envoiPossible();

        assertDoesNotThrow(() -> messageService.envoyerMessage(payload, "alice@test.com"));
    }

    @Test
    @DisplayName("W1 — le locataire actuel écrit en premier à son propriétaire → créée")
    void envoyerMessage_locataireActuelVersProprietaire_cree() {
        User carol = carol();
        MessagePayload payload = new MessagePayload("Bonjour", null, 1L);

        when(userService.getUserByEmail("carol@test.com")).thenReturn(carol);
        when(bienService.findById(1L)).thenReturn(bienDeBob(carol));
        when(conversationService.findConversationWithBienIdAndEmailExpediteur(1L, "carol@test.com", "bob@test.com"))
                .thenReturn(null);
        when(conversationService.createConversation(1L, "carol@test.com", "bob@test.com")).thenReturn(conversation);
        envoiPossible();

        assertDoesNotThrow(() -> messageService.envoyerMessage(payload, "carol@test.com"));

        verify(messagingTemplate).convertAndSendToUser(eq("bob@test.com"), eq("/queue/messages"), eq(messageDTO));
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
    @DisplayName("marquerConversationLue() — seulement la conversation de ce bien (W6)")
    void marquerConversationLue_parConversation() {
        when(conversationService.findConversationWithBienIdAndEmailExpediteur(1L, "alice@test.com", "bob@test.com"))
                .thenReturn(conversation);

        messageService.marquerConversationLue(1L, "Alice@Test.com", " bob@test.com");

        verify(messageRepository).marquerConversationLue(10L, "alice@test.com");
    }

    @Test
    @DisplayName("marquerConversationLue() — pas de conversation sur ce bien → rien n'est modifié (W6)")
    void marquerConversationLue_sansConversation_rienAFaire() {
        when(conversationService.findConversationWithBienIdAndEmailExpediteur(2L, "alice@test.com", "bob@test.com"))
                .thenReturn(null);

        messageService.marquerConversationLue(2L, "alice@test.com", "bob@test.com");

        verify(messageRepository, never()).marquerConversationLue(anyLong(), anyString());
    }
}
