package com.kupanga.api.immobilier.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.kupanga.api.authentification.service.impl.UserDetailsServiceImpl;
import com.kupanga.api.authentification.utils.JwtUtils;
import com.kupanga.api.config.SecurityConfig;
import com.kupanga.api.exception.business.KupangaBusinessException;
import com.kupanga.api.immobilier.dto.readDTO.ContratDTO;
import com.kupanga.api.immobilier.research.ContratSearchService;
import com.kupanga.api.immobilier.research.dto.ContratPageDTO;
import com.kupanga.api.immobilier.research.dto.ContratSearchDTO;
import com.kupanga.api.immobilier.service.ContratService;
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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ContratController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = true)
@DisplayName("Tests ContratController")
class ContratControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean private ContratService        contratService;
    @MockBean private ContratSearchService  contratSearchService;
    @MockBean private JwtUtils              jwtUtils;
    @MockBean private UserDetailsServiceImpl userDetailsService;
    @MockBean private EntityManagerFactory  entityManagerFactory;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    // ─────────────────────────────────────────────────────────────
    // POST /contrats
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /contrats — succès : contrat créé (204)")
    @WithMockUser(username = "proprio@test.com")
    void creerContrat_success_shouldReturn204() throws Exception {
        doNothing().when(contratService).creerContrat(any(), anyString());

        String body = """
                {
                    "bienId": 1,
                    "emailLocataire": "locataire@test.com",
                    "dateDebut": "2030-01-01",
                    "dureeBailMois": 12,
                    "loyerMensuel": 850.00,
                    "chargesMensuelles": 50.00,
                    "depotGarantie": 1700.00
                }
                """;

        mockMvc.perform(post("/contrats")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("POST /contrats — données invalides : 400")
    @WithMockUser(username = "proprio@test.com")
    void creerContrat_invalidBody_shouldReturn400() throws Exception {
        mockMvc.perform(post("/contrats")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    // ─────────────────────────────────────────────────────────────
    // POST /contrats/{id}/signer-proprio
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /contrats/{id}/signer-proprio — succès : 204")
    @WithMockUser(username = "proprio@test.com")
    void signerProprietaire_success_shouldReturn204() throws Exception {
        doNothing().when(contratService).signerProprietaire(eq(1L), anyString(), anyString());

        String signature = "A".repeat(200);
        String body = String.format("{\"signatureBase64\": \"%s\"}", signature);

        mockMvc.perform(post("/contrats/1/signer-proprio")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("POST /contrats/{id}/signer-proprio — contrat introuvable : 404")
    @WithMockUser(username = "proprio@test.com")
    void signerProprietaire_notFound_shouldReturn404() throws Exception {
        doThrow(new KupangaBusinessException("Contrat introuvable", HttpStatus.NOT_FOUND))
                .when(contratService).signerProprietaire(eq(99L), anyString(), anyString());

        String signature = "A".repeat(200);
        String body = String.format("{\"signatureBase64\": \"%s\"}", signature);

        mockMvc.perform(post("/contrats/99/signer-proprio")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound());
    }

    // ─────────────────────────────────────────────────────────────
    // GET /contrats/signer/{token}
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /contrats/signer/{token} — succès : retourne contrat (200)")
    void getContratParToken_success_shouldReturn200() throws Exception {
        ContratDTO dto = new ContratDTO(
                1L, 1L, "75 Boulevard Jules Verne", null, null,
                850.0, 50.0, 1700.0,
                java.time.LocalDate.of(2030, 1, 1), null, 12,
                false, false, null, null,
                null, com.kupanga.api.immobilier.entity.StatutContrat.EN_ATTENTE_SIGNATURE_PROPRIO,
                null, null
        );
        when(contratService.getContratParToken("valid-token")).thenReturn(dto);

        mockMvc.perform(get("/contrats/signer/valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @DisplayName("GET /contrats/signer/{token} — token invalide : 404")
    void getContratParToken_notFound_shouldReturn404() throws Exception {
        when(contratService.getContratParToken("bad-token"))
                .thenThrow(new KupangaBusinessException("Token invalide", HttpStatus.NOT_FOUND));

        mockMvc.perform(get("/contrats/signer/bad-token"))
                .andExpect(status().isNotFound());
    }

    // ─────────────────────────────────────────────────────────────
    // POST /contrats/signer/{token}
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /contrats/signer/{token} — succès : 204")
    void signerLocataire_success_shouldReturn204() throws Exception {
        doNothing().when(contratService).signerLocataire(anyString(), anyString());

        String signature = "B".repeat(200);
        String body = String.format("{\"signatureBase64\": \"%s\"}", signature);

        mockMvc.perform(post("/contrats/signer/valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNoContent());
    }

    // ─────────────────────────────────────────────────────────────
    // POST /contrats/search
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /contrats/search — succès : page de contrats (200)")
    @WithMockUser(username = "user@test.com")
    void search_success_shouldReturn200() throws Exception {
        ContratPageDTO page = new ContratPageDTO(Collections.emptyList(), 0, 0, 0, true, true);
        when(contratSearchService.rechercher(any(ContratSearchDTO.class), anyString())).thenReturn(page);

        mockMvc.perform(post("/contrats/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }
}
