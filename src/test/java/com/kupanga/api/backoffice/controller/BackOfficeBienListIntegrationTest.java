package com.kupanga.api.backoffice.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Liste des biens du back-office : rendu Thymeleaf réel avec le filtre par pays.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@Transactional
@DisplayName("Tests d'intégration — liste des biens du back-office")
class BackOfficeBienListIntegrationTest {

    @Autowired private MockMvc mockMvc;

    @Test
    @WithMockUser(username = "admin@kupanga.test", authorities = "ROLE_BACKOFFICE_ADMIN")
    @DisplayName("J1 : filtre pays en liste déroulante, valeur choisie conservée")
    void liste_filtrePays() throws Exception {
        mockMvc.perform(get("/backoffice/biens").param("pays", "CD"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("name=\"pays\"")))
                .andExpect(content().string(matchesPattern(
                        "(?s).*<option value=\"CD\"\\s+selected=\"selected\">République démocratique du Congo</option>.*")));
    }

    @Test
    @WithMockUser(username = "admin@kupanga.test", authorities = "ROLE_BACKOFFICE_ADMIN")
    @DisplayName("B12 : plus de route de suppression des biens ; l'archivage redirige avec un message")
    void archiver_remplaceSupprimer() throws Exception {
        mockMvc.perform(post("/backoffice/biens/999/supprimer").with(csrf()))
                .andExpect(status().is4xxClientError());
        mockMvc.perform(post("/backoffice/biens/999/archiver").with(csrf()))
                .andExpect(redirectedUrl("/backoffice/biens"))
                .andExpect(flash().attribute("message", "Bien introuvable."));
    }
}
