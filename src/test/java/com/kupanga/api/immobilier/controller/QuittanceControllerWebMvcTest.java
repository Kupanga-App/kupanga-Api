package com.kupanga.api.immobilier.controller;

import com.kupanga.api.authentification.service.impl.UserDetailsServiceImpl;
import com.kupanga.api.authentification.utils.JwtUtils;
import com.kupanga.api.config.SecurityConfig;
import com.kupanga.api.exception.business.KupangaBusinessException;
import com.kupanga.api.immobilier.dto.readDTO.QuittanceDTO;
import com.kupanga.api.immobilier.research.QuittanceSearchService;
import com.kupanga.api.immobilier.research.dto.QuittancePageDTO;
import com.kupanga.api.immobilier.research.dto.QuittanceSearchDTO;
import com.kupanga.api.immobilier.service.QuittanceService;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
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

@WebMvcTest(QuittanceController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = true)
@DisplayName("Tests QuittanceController")
class QuittanceControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean private QuittanceService        quittanceService;
    @MockBean private QuittanceSearchService  quittanceSearchService;
    @MockBean private JwtUtils                jwtUtils;
    @MockBean private UserDetailsServiceImpl   userDetailsService;
    @MockBean private EntityManagerFactory    entityManagerFactory;

    // ─────────────────────────────────────────────────────────────
    // POST /quittances
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /quittances — succès : quittance créée (204)")
    @WithMockUser(username = "proprio@test.com")
    void creerQuittance_success_shouldReturn204() throws Exception {
        doNothing().when(quittanceService).creerQuittance(any(), anyString());

        String body = """
                {
                    "bienId": 1,
                    "emailLocataire": "locataire@test.com",
                    "mois": "3",
                    "annee": 2026,
                    "loyerMensuel": 850.00,
                    "chargesMensuelles": 50.00,
                    "dateEcheance": "2026-03-05"
                }
                """;

        mockMvc.perform(post("/quittances")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("POST /quittances — données invalides : 400")
    @WithMockUser(username = "proprio@test.com")
    void creerQuittance_invalidBody_shouldReturn400() throws Exception {
        mockMvc.perform(post("/quittances")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    // ─────────────────────────────────────────────────────────────
    // POST /quittances/{id}/marquer-payee
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /quittances/{id}/marquer-payee — succès : 204")
    @WithMockUser(username = "proprio@test.com")
    void marquerPayee_success_shouldReturn204() throws Exception {
        doNothing().when(quittanceService).marquerPayee(eq(1L), anyString(), anyString());

        String signature = "E".repeat(200);
        String body = String.format("{\"signatureBase64\": \"%s\"}", signature);

        mockMvc.perform(post("/quittances/1/marquer-payee")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("POST /quittances/{id}/marquer-payee — quittance introuvable : 404")
    @WithMockUser(username = "proprio@test.com")
    void marquerPayee_notFound_shouldReturn404() throws Exception {
        doThrow(new KupangaBusinessException("Quittance introuvable", HttpStatus.NOT_FOUND))
                .when(quittanceService).marquerPayee(eq(99L), anyString(), anyString());

        String signature = "E".repeat(200);
        String body = String.format("{\"signatureBase64\": \"%s\"}", signature);

        mockMvc.perform(post("/quittances/99/marquer-payee")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound());
    }

    // ─────────────────────────────────────────────────────────────
    // GET /quittances/bien/{bienId}
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /quittances/bien/{bienId} — succès : liste quittances (200)")
    @WithMockUser(username = "proprio@test.com")
    void getQuittancesParBien_success_shouldReturn200() throws Exception {
        QuittanceDTO dto = new QuittanceDTO();
        dto.setId(1L);
        dto.setMois("3");
        dto.setAnnee(2026);

        when(quittanceService.getQuittancesParBien(eq(1L), anyString()))
                .thenReturn(List.of(dto));

        mockMvc.perform(get("/quittances/bien/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].mois").value("3"));
    }

    @Test
    @DisplayName("GET /quittances/bien/{bienId} — bien introuvable : 404")
    @WithMockUser(username = "proprio@test.com")
    void getQuittancesParBien_notFound_shouldReturn404() throws Exception {
        when(quittanceService.getQuittancesParBien(eq(99L), anyString()))
                .thenThrow(new KupangaBusinessException("Bien introuvable", HttpStatus.NOT_FOUND));

        mockMvc.perform(get("/quittances/bien/99"))
                .andExpect(status().isNotFound());
    }

    // ─────────────────────────────────────────────────────────────
    // GET /quittances/mes-quittances
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /quittances/mes-quittances — succès : liste (200)")
    @WithMockUser(username = "locataire@test.com")
    void getMesQuittances_success_shouldReturn200() throws Exception {
        when(quittanceService.getQuittancesParLocataire(anyString()))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/quittances/mes-quittances"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    // ─────────────────────────────────────────────────────────────
    // GET /quittances/{id}
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /quittances/{id} — succès : détail quittance (200)")
    @WithMockUser(username = "user@test.com")
    void getQuittanceById_success_shouldReturn200() throws Exception {
        QuittanceDTO dto = new QuittanceDTO();
        dto.setId(1L);
        dto.setAnnee(2026);

        when(quittanceService.getQuittanceById(eq(1L), anyString())).thenReturn(dto);

        mockMvc.perform(get("/quittances/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @DisplayName("GET /quittances/{id} — quittance introuvable : 404")
    @WithMockUser(username = "user@test.com")
    void getQuittanceById_notFound_shouldReturn404() throws Exception {
        when(quittanceService.getQuittanceById(eq(99L), anyString()))
                .thenThrow(new KupangaBusinessException("Quittance introuvable", HttpStatus.NOT_FOUND));

        mockMvc.perform(get("/quittances/99"))
                .andExpect(status().isNotFound());
    }

    // ─────────────────────────────────────────────────────────────
    // POST /quittances/search
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /quittances/search — succès : page quittances (200)")
    @WithMockUser(username = "user@test.com")
    void search_success_shouldReturn200() throws Exception {
        QuittancePageDTO page = new QuittancePageDTO(Collections.emptyList(), 0, 0, 0, true, true);
        when(quittanceSearchService.rechercher(any(QuittanceSearchDTO.class), anyString()))
                .thenReturn(page);

        mockMvc.perform(post("/quittances/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }
}
