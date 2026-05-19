package com.kupanga.api.chat.service;

import com.kupanga.api.chat.entity.Conversation;
import com.kupanga.api.chat.repository.ConversationRepository;
import com.kupanga.api.chat.service.impl.ConversationServiceImpl;
import com.kupanga.api.exception.business.KupangaBusinessException;
import com.kupanga.api.immobilier.entity.Bien;
import com.kupanga.api.immobilier.service.BienService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.http.HttpStatus;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("Tests unitaires — ConversationServiceImpl")
class ConversationServiceImplTest {

    @Mock private BienService              bienService;
    @Mock private ConversationRepository   conversationRepository;

    @InjectMocks
    private ConversationServiceImpl conversationService;

    private Bien bien;
    private Conversation conversation;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        bien = Bien.builder()
                .id(1L)
                .titre("Appartement Nantes")
                .build();

        conversation = Conversation.builder()
                .id(10L)
                .bien(bien)
                .emailExpediteur("alice@test.com")
                .emailDestinataire("bob@test.com")
                .build();
    }

    // ══════════════════════════════════════════════════════════════
    // save
    // ══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("save() — délègue au repository")
    void save_delegatesToRepository() {
        when(conversationRepository.save(conversation)).thenReturn(conversation);

        conversationService.save(conversation);

        verify(conversationRepository).save(conversation);
    }

    // ══════════════════════════════════════════════════════════════
    // createConversation
    // ══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("createConversation() — succès : bien trouvé, conversation créée et sauvegardée")
    void createConversation_success() {
        when(bienService.findById(1L)).thenReturn(bien);
        when(conversationRepository.save(any(Conversation.class))).thenReturn(conversation);

        Conversation result = conversationService.createConversation(1L, "alice@test.com", "bob@test.com");

        assertThat(result.getId()).isEqualTo(10L);
        assertThat(result.getBien()).isEqualTo(bien);
        verify(conversationRepository).save(any(Conversation.class));
    }

    @Test
    @DisplayName("createConversation() — bien introuvable → exception propagée")
    void createConversation_bienNotFound_propagatesException() {
        when(bienService.findById(99L))
                .thenThrow(new KupangaBusinessException("Bien introuvable", HttpStatus.NOT_FOUND));

        assertThrows(KupangaBusinessException.class,
                () -> conversationService.createConversation(99L, "alice@test.com", "bob@test.com"));

        verify(conversationRepository, never()).save(any());
    }

    // ══════════════════════════════════════════════════════════════
    // findConversationWithBienIdAndEmailExpediteur
    // ══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("findConversation...() — conversation existante → retournée")
    void findConversation_exists_returnsConversation() {
        when(conversationRepository.findConversationWithBienIdAndEmailExpediteur(1L, "alice@test.com", "bob@test.com"))
                .thenReturn(Optional.of(conversation));

        Conversation result = conversationService
                .findConversationWithBienIdAndEmailExpediteur(1L, "alice@test.com", "bob@test.com");

        assertThat(result).isEqualTo(conversation);
    }

    @Test
    @DisplayName("findConversation...() — conversation absente → null retourné")
    void findConversation_absent_returnsNull() {
        when(conversationRepository.findConversationWithBienIdAndEmailExpediteur(1L, "alice@test.com", "bob@test.com"))
                .thenReturn(Optional.empty());

        Conversation result = conversationService
                .findConversationWithBienIdAndEmailExpediteur(1L, "alice@test.com", "bob@test.com");

        assertThat(result).isNull();
    }
}
