package com.kupanga.api.immobilier.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.kupanga.api.authentification.service.impl.UserDetailsServiceImpl;
import com.kupanga.api.authentification.utils.JwtUtils;
import com.kupanga.api.config.SecurityConfig;
import com.kupanga.api.exception.business.KupangaBusinessException;
import com.kupanga.api.immobilier.dto.formDTO.BienFormDTO;
import com.kupanga.api.immobilier.dto.formDTO.BienUpdateDTO;
import com.kupanga.api.immobilier.dto.readDTO.BienDTO;
import com.kupanga.api.immobilier.entity.TypeBien;
import com.kupanga.api.immobilier.research.BienSearchService;
import com.kupanga.api.immobilier.research.dto.BienPageDTO;
import com.kupanga.api.immobilier.research.dto.BienSearchDTO;
import com.kupanga.api.immobilier.service.BienService;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BienController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = true)
@DisplayName("Tests BienController")
class BienControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean private BienService         bienService;
    @MockBean private BienSearchService   bienSearchService;
    @MockBean private JwtUtils            jwtUtils;
    @MockBean private UserDetailsServiceImpl userDetailsService;
    @MockBean private EntityManagerFactory entityManagerFactory;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    // ─────────────────────────────────────────────────────────────
    // POST /biens (multipart)
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /biens — succès : bien créé (204)")
    @WithMockUser(username = "proprietaire@test.com", roles = "PROPRIETAIRE")
    void createBien_success_shouldReturn204() throws Exception {
        doNothing().when(bienService).createBien(any(), any(), any());

        BienFormDTO dto = BienFormDTO.builder()
                .titre("Appartement T3")
                .typeBien(TypeBien.APPARTEMENT)
                .adresse("12 rue des Tests")
                .ville("Nantes")
                .codePostal("44000")
                .pays("France")
                .surfaceHabitable(65.0)
                .nombrePieces(3)
                .loyerMensuel(850.0)
                .chargesMensuelles(50.0)
                .depotGarantie(1700.0)
                .meuble(false)
                .colocation(false)
                .disponibleDe(java.time.LocalDate.of(2030, 1, 1))
                .build();

        MockMultipartFile bienPart = new MockMultipartFile(
                "bienFormDTO", "", "application/json",
                objectMapper.writeValueAsBytes(dto));

        MockMultipartFile image = new MockMultipartFile(
                "files", "photo.png", "image/png", "fake-img".getBytes());

        mockMvc.perform(multipart("/biens").file(bienPart).file(image))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("POST /biens — sans image : 204 quand required=false")
    @WithMockUser(username = "proprietaire@test.com")
    void createBien_withoutFiles_shouldReturn204() throws Exception {
        doNothing().when(bienService).createBien(any(), any(), any());

        MockMultipartFile bienPart = new MockMultipartFile(
                "bienFormDTO", "", "application/json",
                objectMapper.writeValueAsBytes(new BienFormDTO()));

        mockMvc.perform(multipart("/biens").file(bienPart))
                .andExpect(status().isNoContent());
    }

    // ─────────────────────────────────────────────────────────────
    // GET /biens/{id}
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /biens/{id} — succès : retourne le bien (200)")
    @WithMockUser(username = "user@test.com")
    void getBienInfos_success_shouldReturn200() throws Exception {
        BienDTO dto = BienDTO.builder()
                .id(1L)
                .titre("Appartement T3")
                .typeBien(TypeBien.APPARTEMENT)
                .ville("Nantes")
                .build();

        when(bienService.getBienInfos(1L)).thenReturn(dto);

        mockMvc.perform(get("/biens/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.titre").value("Appartement T3"));
    }

    @Test
    @DisplayName("GET /biens/{id} — bien introuvable : 404")
    @WithMockUser(username = "user@test.com")
    void getBienInfos_notFound_shouldReturn404() throws Exception {
        when(bienService.getBienInfos(99L))
                .thenThrow(new KupangaBusinessException("Bien introuvable", HttpStatus.NOT_FOUND));

        mockMvc.perform(get("/biens/99"))
                .andExpect(status().isNotFound());
    }

    // ─────────────────────────────────────────────────────────────
    // POST /biens/search
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /biens/search — succès : retourne page de biens (200)")
    @WithMockUser(username = "user@test.com")
    void rechercher_success_shouldReturn200() throws Exception {
        BienPageDTO page = new BienPageDTO(Collections.emptyList(), 0, 0, 0, true, true);
        when(bienSearchService.rechercher(any(BienSearchDTO.class))).thenReturn(page);

        mockMvc.perform(post("/biens/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    // ─────────────────────────────────────────────────────────────
    // PATCH /biens/{id}
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("PATCH /biens/{id} — succès : retourne bien mis à jour (200)")
    @WithMockUser(username = "proprietaire@test.com")
    void updateBien_success_shouldReturn200() throws Exception {
        BienDTO updated = BienDTO.builder()
                .id(1L)
                .titre("Studio rénové")
                .typeBien(TypeBien.STUDIO)
                .build();

        when(bienService.updateBien(any(), eq(1L), any(BienUpdateDTO.class))).thenReturn(updated);

        String body = """
                { "typeBien": "STUDIO", "titre": "Studio rénové" }
                """;

        mockMvc.perform(patch("/biens/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titre").value("Studio rénové"));
    }

    @Test
    @DisplayName("PATCH /biens/{id} — body invalide : 400")
    @WithMockUser(username = "proprietaire@test.com")
    void updateBien_invalidBody_shouldReturn400() throws Exception {
        mockMvc.perform(patch("/biens/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid}"))
                .andExpect(status().isBadRequest());
    }

    // ─────────────────────────────────────────────────────────────
    // POST /biens/{bienId}/assigne-locataire/{userId}
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /biens/{bienId}/assigne-locataire/{userId} — succès : 204")
    @WithMockUser(username = "proprietaire@test.com")
    void assignLocataire_success_shouldReturn204() throws Exception {
        doNothing().when(bienService).affectLocataire(any(), eq(1L), eq(2L));

        mockMvc.perform(post("/biens/1/assigne-locataire/2"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("POST /biens/{bienId}/assigne-locataire/{userId} — bien introuvable : 404")
    @WithMockUser(username = "proprietaire@test.com")
    void assignLocataire_notFound_shouldReturn404() throws Exception {
        doThrow(new KupangaBusinessException("Bien introuvable", HttpStatus.NOT_FOUND))
                .when(bienService).affectLocataire(any(), eq(99L), eq(2L));

        mockMvc.perform(post("/biens/99/assigne-locataire/2"))
                .andExpect(status().isNotFound());
    }
}
