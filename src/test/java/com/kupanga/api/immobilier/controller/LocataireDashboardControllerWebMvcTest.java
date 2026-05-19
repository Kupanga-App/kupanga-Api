package com.kupanga.api.immobilier.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.kupanga.api.authentification.service.impl.UserDetailsServiceImpl;
import com.kupanga.api.authentification.utils.JwtUtils;
import com.kupanga.api.config.SecurityConfig;
import com.kupanga.api.exception.business.KupangaBusinessException;
import com.kupanga.api.immobilier.dto.readDTO.LocataireDashboardDTO;
import com.kupanga.api.immobilier.service.LocataireDashboardService;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(LocataireDashboardController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = true)
@DisplayName("Tests LocataireDashboardController")
class LocataireDashboardControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean private LocataireDashboardService dashboardService;
    @MockBean private JwtUtils                  jwtUtils;
    @MockBean private UserDetailsServiceImpl    userDetailsService;
    @MockBean private EntityManagerFactory      entityManagerFactory;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    // ─────────────────────────────────────────────────────────────
    // GET /locataire/dashboard/{bienId}
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /locataire/dashboard/{bienId} — succès : retourne le dashboard (200)")
    @WithMockUser(username = "locataire@test.com", roles = "LOCATAIRE")
    void getDashboard_success_shouldReturn200() throws Exception {

        LocataireDashboardDTO dto = buildStubDashboard();
        when(dashboardService.getDashboard(any(), eq(10L))).thenReturn(dto);

        mockMvc.perform(get("/locataire/dashboard/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.locataire.prenom").value("Alice"))
                .andExpect(jsonPath("$.locataire.nom").value("Dupont"))
                .andExpect(jsonPath("$.locataire.initiales").value("AD"))
                .andExpect(jsonPath("$.bien.id").value(10))
                .andExpect(jsonPath("$.bien.adresse").value("12 rue des Tests, 44000 Nantes"))
                .andExpect(jsonPath("$.bien.type").value("APPARTEMENT"))
                .andExpect(jsonPath("$.contrat.id").value(100))
                .andExpect(jsonPath("$.contrat.type").value("MEUBLE"))
                .andExpect(jsonPath("$.contrat.loyerMensuel").value(850.0))
                .andExpect(jsonPath("$.contrat.statut").value("SIGNE"))
                .andExpect(jsonPath("$.statuts.derniereQuittance").value("Mars 2024"))
                .andExpect(jsonPath("$.quittances").isArray())
                .andExpect(jsonPath("$.etatsDesLieux").isArray());
    }

    @Test
    @DisplayName("GET /locataire/dashboard/{bienId} — bien introuvable : 404")
    @WithMockUser(username = "locataire@test.com", roles = "LOCATAIRE")
    void getDashboard_bienNotFound_shouldReturn404() throws Exception {

        when(dashboardService.getDashboard(any(), eq(99L)))
                .thenThrow(new KupangaBusinessException("Bien introuvable", HttpStatus.NOT_FOUND));

        mockMvc.perform(get("/locataire/dashboard/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /locataire/dashboard/{bienId} — utilisateur non locataire du bien : 403")
    @WithMockUser(username = "autre@test.com", roles = "PROPRIETAIRE")
    void getDashboard_notLocataire_shouldReturn403() throws Exception {

        when(dashboardService.getDashboard(any(), eq(10L)))
                .thenThrow(new KupangaBusinessException(
                        "Accès refusé : vous n'êtes pas le locataire de ce bien",
                        HttpStatus.FORBIDDEN));

        mockMvc.perform(get("/locataire/dashboard/10"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /locataire/dashboard/{bienId} — contrat null (pas de contrat actif) : 200 avec contrat absent")
    @WithMockUser(username = "locataire@test.com", roles = "LOCATAIRE")
    void getDashboard_noActiveContrat_contratNodeIsNull() throws Exception {

        LocataireDashboardDTO dto = buildStubDashboard();
        dto.setContrat(null);
        when(dashboardService.getDashboard(any(), eq(10L))).thenReturn(dto);

        mockMvc.perform(get("/locataire/dashboard/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contrat").doesNotExist());
    }

    // ─────────────────────────────────────────────────────────────
    // Fixture
    // ─────────────────────────────────────────────────────────────

    private LocataireDashboardDTO buildStubDashboard() {

        LocataireDashboardDTO dto = new LocataireDashboardDTO();

        LocataireDashboardDTO.LocataireDTO locataireDTO = new LocataireDashboardDTO.LocataireDTO();
        locataireDTO.setPrenom("Alice");
        locataireDTO.setNom("Dupont");
        locataireDTO.setInitiales("AD");
        dto.setLocataire(locataireDTO);

        LocataireDashboardDTO.BienResumeDTO bienDTO = new LocataireDashboardDTO.BienResumeDTO();
        bienDTO.setId(10L);
        bienDTO.setAdresse("12 rue des Tests, 44000 Nantes");
        bienDTO.setSurface(65.0);
        bienDTO.setNbPieces(3);
        bienDTO.setType("APPARTEMENT");
        bienDTO.setNomProprietaire("Jean Martin");
        bienDTO.setPhotos(List.of());
        bienDTO.setColocation(false);
        dto.setBien(bienDTO);

        LocataireDashboardDTO.ContratResumeDTO contratDTO = new LocataireDashboardDTO.ContratResumeDTO();
        contratDTO.setId(100L);
        contratDTO.setType("MEUBLE");
        contratDTO.setDateDebut(LocalDate.of(2023, 9, 1));
        contratDTO.setDateFin(LocalDate.of(2024, 8, 31));
        contratDTO.setMoisEcoules(20);
        contratDTO.setDureeTotale(12);
        contratDTO.setLoyerMensuel(850.0);
        contratDTO.setCharges(80.0);
        contratDTO.setDepotGarantie(1700.0);
        contratDTO.setStatut(com.kupanga.api.immobilier.entity.StatutContrat.SIGNE);
        dto.setContrat(contratDTO);

        LocataireDashboardDTO.StatutsDTO statuts = new LocataireDashboardDTO.StatutsDTO();
        statuts.setDerniereQuittance("Mars 2024");
        statuts.setDateFinContrat(LocalDate.of(2024, 8, 31));
        dto.setStatuts(statuts);

        dto.setQuittances(List.of());
        dto.setEtatsDesLieux(List.of());

        return dto;
    }
}
