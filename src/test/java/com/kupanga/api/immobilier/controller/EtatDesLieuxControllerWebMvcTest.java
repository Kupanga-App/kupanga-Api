package com.kupanga.api.immobilier.controller;

import com.kupanga.api.authentification.service.impl.UserDetailsServiceImpl;
import com.kupanga.api.authentification.utils.JwtUtils;
import com.kupanga.api.config.SecurityConfig;
import com.kupanga.api.exception.business.KupangaBusinessException;
import com.kupanga.api.immobilier.dto.readDTO.EtatDesLieuxDTO;
import com.kupanga.api.immobilier.entity.StatutEdl;
import com.kupanga.api.immobilier.entity.TypeEtat;
import com.kupanga.api.immobilier.research.EtatDesLieuxSearchService;
import com.kupanga.api.immobilier.research.dto.EtatDesLieuxPageDTO;
import com.kupanga.api.immobilier.research.dto.EtatDesLieuxSearchDTO;
import com.kupanga.api.immobilier.service.EtatDesLieuxService;
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

@WebMvcTest(EtatDesLieuxController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = true)
@DisplayName("Tests EtatDesLieuxController")
class EtatDesLieuxControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean private EtatDesLieuxService        edlService;
    @MockBean private EtatDesLieuxSearchService  edlSearchService;
    @MockBean private JwtUtils                   jwtUtils;
    @MockBean private UserDetailsServiceImpl      userDetailsService;
    @MockBean private EntityManagerFactory       entityManagerFactory;

    // ─────────────────────────────────────────────────────────────
    // POST /etats-des-lieux
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /etats-des-lieux — succès : EDL créé (201)")
    @WithMockUser(username = "proprio@test.com")
    void creerEdl_success_shouldReturn201() throws Exception {
        doNothing().when(edlService).creerEtatDesLieux(any(), anyString());

        String body = """
                {
                    "bienId": 1,
                    "emailLocataire": "locataire@test.com",
                    "type": "ENTREE",
                    "dateRealisation": "2030-01-01"
                }
                """;

        mockMvc.perform(post("/etats-des-lieux")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("POST /etats-des-lieux — données invalides : 400")
    @WithMockUser(username = "proprio@test.com")
    void creerEdl_invalidBody_shouldReturn400() throws Exception {
        mockMvc.perform(post("/etats-des-lieux")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    // ─────────────────────────────────────────────────────────────
    // POST /etats-des-lieux/{id}/signer-proprietaire
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /etats-des-lieux/{id}/signer-proprietaire — succès : 204")
    @WithMockUser(username = "proprio@test.com")
    void signerProprietaire_success_shouldReturn204() throws Exception {
        doNothing().when(edlService).signerProprietaire(eq(1L), anyString(), anyString());

        String signature = "C".repeat(200);
        String body = String.format("{\"signatureBase64\": \"%s\"}", signature);

        mockMvc.perform(post("/etats-des-lieux/1/signer-proprietaire")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("POST /etats-des-lieux/{id}/signer-proprietaire — EDL introuvable : 404")
    @WithMockUser(username = "proprio@test.com")
    void signerProprietaire_notFound_shouldReturn404() throws Exception {
        doThrow(new KupangaBusinessException("EDL introuvable", HttpStatus.NOT_FOUND))
                .when(edlService).signerProprietaire(eq(99L), anyString(), anyString());

        String signature = "C".repeat(200);
        String body = String.format("{\"signatureBase64\": \"%s\"}", signature);

        mockMvc.perform(post("/etats-des-lieux/99/signer-proprietaire")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound());
    }

    // ─────────────────────────────────────────────────────────────
    // GET /etats-des-lieux/signer/{token}
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /etats-des-lieux/signer/{token} — succès : retourne EDL (200)")
    void getEdlParToken_success_shouldReturn200() throws Exception {
        EtatDesLieuxDTO dto = new EtatDesLieuxDTO();
        dto.setId(1L);
        dto.setType(TypeEtat.ENTREE);
        dto.setStatut(StatutEdl.EN_ATTENTE_SIGNATURE_LOCATAIRE);

        when(edlService.getEdlParToken("valid-token")).thenReturn(dto);

        mockMvc.perform(get("/etats-des-lieux/signer/valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.type").value("ENTREE"));
    }

    @Test
    @DisplayName("GET /etats-des-lieux/signer/{token} — token invalide : 401")
    void getEdlParToken_invalid_shouldReturn401() throws Exception {
        when(edlService.getEdlParToken("bad-token"))
                .thenThrow(new KupangaBusinessException("Token invalide", HttpStatus.UNAUTHORIZED));

        mockMvc.perform(get("/etats-des-lieux/signer/bad-token"))
                .andExpect(status().isUnauthorized());
    }

    // ─────────────────────────────────────────────────────────────
    // POST /etats-des-lieux/signer/{token}
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /etats-des-lieux/signer/{token} — succès : 204")
    void signerLocataire_success_shouldReturn204() throws Exception {
        doNothing().when(edlService).signerLocataire(anyString(), anyString());

        String signature = "D".repeat(200);
        String body = String.format("{\"signatureBase64\": \"%s\"}", signature);

        mockMvc.perform(post("/etats-des-lieux/signer/valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNoContent());
    }

    // ─────────────────────────────────────────────────────────────
    // POST /etats-des-lieux/search
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /etats-des-lieux/search — succès : page EDL (200)")
    @WithMockUser(username = "user@test.com")
    void search_success_shouldReturn200() throws Exception {
        EtatDesLieuxPageDTO page = new EtatDesLieuxPageDTO(Collections.emptyList(), 0, 0, 0, true, true);
        when(edlSearchService.rechercher(any(EtatDesLieuxSearchDTO.class), anyString())).thenReturn(page);

        mockMvc.perform(post("/etats-des-lieux/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }
}
