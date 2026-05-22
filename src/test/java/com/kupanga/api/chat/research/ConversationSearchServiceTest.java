package com.kupanga.api.chat.research;

import com.kupanga.api.chat.dto.ConversationDTO;
import com.kupanga.api.chat.dto.ConversationPageDTO;
import com.kupanga.api.chat.dto.ConversationSearchDTO;
import com.kupanga.api.chat.entity.Conversation;
import com.kupanga.api.chat.mapper.ConversationMapper;
import com.kupanga.api.chat.repository.ConversationRepository;
import com.kupanga.api.chat.repository.MessageRepository;
import com.kupanga.api.chat.research.specification.ConversationSpecification;
import com.kupanga.api.user.entity.User;
import com.kupanga.api.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@DisplayName("Tests unitaires — ConversationSearchService")
@SuppressWarnings("unchecked")
class ConversationSearchServiceTest {

    @Mock private ConversationRepository    conversationRepository;
    @Mock private ConversationSpecification conversationSpecification;
    @Mock private ConversationMapper        conversationMapper;
    @Mock private MessageRepository         messageRepository;
    @Mock private UserService               userService;

    @InjectMocks
    private ConversationSearchService conversationSearchService;

    private User user;
    private User userBob;
    private Conversation conversation;
    private ConversationDTO baseDTO;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        user = User.builder()
                .id(1L)
                .mail("alice@test.com")
                .firstName("Alice")
                .lastName("Martin")
                .build();

        userBob = User.builder()
                .id(2L)
                .mail("bob@test.com")
                .firstName("Bob")
                .lastName("Dupont")
                .build();

        conversation = Conversation.builder()
                .id(10L)
                .emailExpediteur("alice@test.com")
                .emailDestinataire("bob@test.com")
                .lastMessage("Bonjour")
                .lastMessageAt(LocalDateTime.now())
                .build();

        baseDTO = new ConversationDTO(10L, 1L, "Appart T3",
                "alice@test.com", "bob@test.com",
                "Alice Martin", "Bob Dupont",
                null, null,
                "Bonjour", LocalDateTime.now(), LocalDateTime.now(), 0L);
    }

    @Test
    @DisplayName("rechercher() — retourne la page de conversations avec nonLuCount calculé")
    void rechercher_returnsConversationPage() {
        ConversationSearchDTO dto = new ConversationSearchDTO(null, null,
                0, 10, null, Sort.Direction.ASC);

        when(userService.getUserByEmail("alice@test.com")).thenReturn(user);
        when(userService.getUserByEmail("bob@test.com")).thenReturn(userBob);
        when(conversationSpecification.build(eq("alice@test.com"), eq(dto)))
                .thenReturn(mock(Specification.class));
        when(conversationRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(conversation)));
        when(conversationMapper.toDto(conversation, user, userBob)).thenReturn(baseDTO);
        when(messageRepository.countNonLuByConversationAndDestinataire(10L, "alice@test.com"))
                .thenReturn(3L);

        ConversationPageDTO result = conversationSearchService.rechercher("alice@test.com", dto);

        assertThat(result).isNotNull();
        assertThat(result.contenu()).hasSize(1);
        assertThat(result.contenu().get(0).nonLuCount()).isEqualTo(3L);
    }

    @Test
    @DisplayName("rechercher() — aucune conversation → page vide")
    void rechercher_noConversations_returnsEmptyPage() {
        ConversationSearchDTO dto = new ConversationSearchDTO(null, null,
                0, 10, null, Sort.Direction.ASC);

        when(userService.getUserByEmail("alice@test.com")).thenReturn(user);
        when(conversationSpecification.build(any(), any())).thenReturn(mock(Specification.class));
        when(conversationRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        ConversationPageDTO result = conversationSearchService.rechercher("alice@test.com", dto);

        assertThat(result.contenu()).isEmpty();
    }
}
