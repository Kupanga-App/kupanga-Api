package com.kupanga.api.chat.controller;

import com.kupanga.api.authentification.service.impl.UserDetailsServiceImpl;
import com.kupanga.api.authentification.utils.JwtUtils;
import com.kupanga.api.chat.dto.ConversationPageDTO;
import com.kupanga.api.chat.dto.ConversationSearchDTO;
import com.kupanga.api.chat.research.ConversationSearchService;
import com.kupanga.api.config.SecurityConfig;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ConversationController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = true)
@DisplayName("Tests ConversationController")
class ConversationControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean private ConversationSearchService conversationSearchService;
    @MockBean private JwtUtils                  jwtUtils;
    @MockBean private UserDetailsServiceImpl     userDetailsService;
    @MockBean private EntityManagerFactory      entityManagerFactory;

    // ─────────────────────────────────────────────────────────────
    // POST /conversations/search
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /conversations/search — succès avec body vide (200)")
    @WithMockUser(username = "user@test.com")
    void rechercherConversations_emptyBody_shouldReturn200() throws Exception {
        ConversationPageDTO page = new ConversationPageDTO(Collections.emptyList(), 0, 0, 0, true, true);
        when(conversationSearchService.rechercher(anyString(), any(ConversationSearchDTO.class)))
                .thenReturn(page);

        mockMvc.perform(post("/conversations/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.contenu").isArray());
    }

    @Test
    @DisplayName("POST /conversations/search — body absent : 200 (dto par défaut)")
    @WithMockUser(username = "user@test.com")
    void rechercherConversations_noBody_shouldReturn200() throws Exception {
        ConversationPageDTO page = new ConversationPageDTO(Collections.emptyList(), 0, 0, 0, true, true);
        when(conversationSearchService.rechercher(anyString(), any(ConversationSearchDTO.class)))
                .thenReturn(page);

        mockMvc.perform(post("/conversations/search")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /conversations/search — avec filtre lu=false : 200")
    @WithMockUser(username = "user@test.com")
    void rechercherConversations_withFilter_shouldReturn200() throws Exception {
        ConversationPageDTO page = new ConversationPageDTO(Collections.emptyList(), 0, 1, 3, false, true);
        when(conversationSearchService.rechercher(anyString(), any(ConversationSearchDTO.class)))
                .thenReturn(page);

        mockMvc.perform(post("/conversations/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "lu": false, "page": 0, "size": 10 }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3));
    }
}
