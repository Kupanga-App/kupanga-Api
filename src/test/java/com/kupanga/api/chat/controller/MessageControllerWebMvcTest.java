package com.kupanga.api.chat.controller;

import com.kupanga.api.authentification.service.impl.UserDetailsServiceImpl;
import com.kupanga.api.authentification.utils.JwtUtils;
import com.kupanga.api.chat.dto.MessageDTO;
import com.kupanga.api.chat.service.MessageService;
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
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MessageController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = true)
@DisplayName("Tests MessageController")
class MessageControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean private MessageService         messageService;
    @MockBean private JwtUtils               jwtUtils;
    @MockBean private UserDetailsServiceImpl  userDetailsService;
    @MockBean private EntityManagerFactory   entityManagerFactory;

    // ─────────────────────────────────────────────────────────────
    // GET /historique
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /historique — succès : retourne liste de messages (200)")
    @WithMockUser(username = "proprio@test.com")
    void getHistorique_success_shouldReturn200() throws Exception {
        List<MessageDTO> messages = List.of(
                MessageDTO.builder()
                        .id(1L)
                        .contenu("Bonjour")
                        .expediteurEmail("proprio@test.com")
                        .build()
        );
        when(messageService.getHistorique(eq(1L), eq("proprio@test.com"), eq("locataire@test.com")))
                .thenReturn(messages);

        mockMvc.perform(get("/historique")
                        .param("bienId", "1")
                        .param("emailInterlocuteur", "locataire@test.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].contenu").value("Bonjour"));
    }

    @Test
    @DisplayName("GET /historique — liste vide : 200 avec []")
    @WithMockUser(username = "proprio@test.com")
    void getHistorique_empty_shouldReturn200() throws Exception {
        when(messageService.getHistorique(anyLong(), anyString(), anyString()))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/historique")
                        .param("bienId", "1")
                        .param("emailInterlocuteur", "locataire@test.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    // ─────────────────────────────────────────────────────────────
    // GET /messages/non-lus
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /messages/non-lus — succès : retourne le compteur (200)")
    @WithMockUser(username = "user@test.com")
    void countNonLus_success_shouldReturn200() throws Exception {
        when(messageService.countMessagesNonLus("user@test.com")).thenReturn(3L);

        mockMvc.perform(get("/messages/non-lus"))
                .andExpect(status().isOk())
                .andExpect(content().string("3"));
    }

    @Test
    @DisplayName("GET /messages/non-lus — zéro messages non lus : 200")
    @WithMockUser(username = "user@test.com")
    void countNonLus_zero_shouldReturn200() throws Exception {
        when(messageService.countMessagesNonLus(anyString())).thenReturn(0L);

        mockMvc.perform(get("/messages/non-lus"))
                .andExpect(status().isOk())
                .andExpect(content().string("0"));
    }

    // ─────────────────────────────────────────────────────────────
    // POST /messages/conversation/{emailExpediteur}/lire
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /messages/conversation/{email}/lire — succès : 200")
    @WithMockUser(username = "user@test.com")
    void marquerLus_success_shouldReturn200() throws Exception {
        doNothing().when(messageService).marquerConversationLue("user@test.com", "locataire@test.com");

        mockMvc.perform(post("/messages/conversation/locataire@test.com/lire")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(messageService).marquerConversationLue("user@test.com", "locataire@test.com");
    }
}
