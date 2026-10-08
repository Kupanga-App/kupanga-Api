package com.kupanga.api.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kupanga.api.authentification.service.impl.UserDetailsServiceImpl;
import com.kupanga.api.authentification.utils.JwtUtils;
import com.kupanga.api.config.SecurityConfig;
import com.kupanga.api.immobilier.dto.readDTO.BienDTO;
import com.kupanga.api.immobilier.entity.TypeBien;
import com.kupanga.api.immobilier.service.BienService;
import com.kupanga.api.user.research.LocataireSearchService;
import com.kupanga.api.user.research.dto.LocatairePageDTO;
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

@WebMvcTest(UserController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = true)
@DisplayName("Tests UserController")
class UserControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean private BienService            bienService;
    @MockBean private LocataireSearchService  locataireSearchService;
    @MockBean private JwtUtils               jwtUtils;
    @MockBean private UserDetailsServiceImpl  userDetailsService;
    @MockBean private EntityManagerFactory   entityManagerFactory;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ─────────────────────────────────────────────────────────────
    // GET /users/biens
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /users/biens/{id} — sans token : 401 (P0-6)")
    void getBienPrive_withoutToken_shouldReturn401() throws Exception {
        mockMvc.perform(get("/users/biens/1"))
                .andExpect(status().isUnauthorized());

        verify(bienService, never()).getBienPrive(any(), any());
    }

    @Test
    @DisplayName("GET /users/biens/{id} — authentifié : 200, contrôle délégué au service")
    @WithMockUser(username = "proprio@test.com", roles = "PROPRIETAIRE")
    void getBienPrive_authenticated_shouldReturn200() throws Exception {
        when(bienService.getBienPrive(1L, "proprio@test.com")).thenReturn(BienDTO.builder().id(1L).build());

        mockMvc.perform(get("/users/biens/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @DisplayName("GET /users/biens — succès : liste des biens (200)")
    @WithMockUser(username = "proprio@test.com")
    void getAllBiens_success_shouldReturn200() throws Exception {
        List<BienDTO> biens = List.of(
                BienDTO.builder().id(1L).titre("Appart T3").typeBien(TypeBien.APPARTEMENT).build()
        );
        when(bienService.findAllPropertiesAssociateToUser("proprio@test.com")).thenReturn(biens);

        mockMvc.perform(get("/users/biens"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].titre").value("Appart T3"));
    }

    @Test
    @DisplayName("GET /users/biens — liste vide : 200 avec []")
    @WithMockUser(username = "proprio@test.com")
    void getAllBiens_empty_shouldReturn200() throws Exception {
        when(bienService.findAllPropertiesAssociateToUser(anyString())).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/users/biens"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    // ─────────────────────────────────────────────────────────────
    // POST /users/{bienId}/recherche-locataire
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /users/{bienId}/recherche-locataire — succès : page locataires (200)")
    @WithMockUser(username = "proprio@test.com", roles = "PROPRIETAIRE")
    void rechercherLocataires_success_shouldReturn200() throws Exception {
        LocatairePageDTO page = new LocatairePageDTO(Collections.emptyList(), 0, 0, 0, true, true);
        when(locataireSearchService.rechercher(anyString(), eq(1L), any())).thenReturn(page);

        mockMvc.perform(post("/users/1/recherche-locataire")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    @DisplayName("POST /users/{bienId}/recherche-locataire — body absent : 200 (dto par défaut)")
    @WithMockUser(username = "proprio@test.com", roles = "PROPRIETAIRE")
    void rechercherLocataires_emptyBody_shouldReturn200() throws Exception {
        LocatairePageDTO page = new LocatairePageDTO(Collections.emptyList(), 0, 1, 5, false, true);
        when(locataireSearchService.rechercher(anyString(), eq(1L), any())).thenReturn(page);

        mockMvc.perform(post("/users/1/recherche-locataire")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /users/1/recherche-locataire — pagination hors bornes (size=0, size=10000, page=-1) : 400, aucune recherche (VALID)")
    @WithMockUser(username = "proprio@test.com", roles = "PROPRIETAIRE")
    void search_paginationHorsBornes_shouldReturn400() throws Exception {
        for (String body : new String[] {"{\"size\": 0}", "{\"size\": 10000}", "{\"page\": -1}"}) {
            mockMvc.perform(post("/users/1/recherche-locataire")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }

        verify(locataireSearchService, never()).rechercher(anyString(), anyLong(), any());
    }
}
