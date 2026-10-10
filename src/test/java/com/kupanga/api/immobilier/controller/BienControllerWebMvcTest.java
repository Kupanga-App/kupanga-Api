package com.kupanga.api.immobilier.controller;

import com.kupanga.api.juridiction.Pays;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.kupanga.api.authentification.service.impl.UserDetailsServiceImpl;
import com.kupanga.api.authentification.utils.JwtUtils;
import com.kupanga.api.config.SecurityConfig;
import com.kupanga.api.exception.business.KupangaBusinessException;
import com.kupanga.api.immobilier.dto.formDTO.BienFormDTO;
import com.kupanga.api.immobilier.dto.formDTO.BienUpdateDTO;
import com.kupanga.api.immobilier.dto.readDTO.BienDTO;
import com.kupanga.api.immobilier.dto.readDTO.BienPublicDTO;
import com.kupanga.api.immobilier.dto.readDTO.ProprietairePublicDTO;
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
import java.math.BigDecimal;

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

        BienFormDTO dto = bienFormValide();

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
    @WithMockUser(username = "proprietaire@test.com", roles = "PROPRIETAIRE")
    void createBien_withoutFiles_shouldReturn204() throws Exception {
        doNothing().when(bienService).createBien(any(), any(), any());

        MockMultipartFile bienPart = new MockMultipartFile(
                "bienFormDTO", "", "application/json",
                objectMapper.writeValueAsBytes(bienFormValide()));

        mockMvc.perform(multipart("/biens").file(bienPart))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("POST /biens — formulaire invalide : 400, aucun bien créé (B4)")
    @WithMockUser(username = "proprietaire@test.com", roles = "PROPRIETAIRE")
    void createBien_invalidForm_shouldReturn400() throws Exception {
        MockMultipartFile bienPart = new MockMultipartFile(
                "bienFormDTO", "", "application/json",
                objectMapper.writeValueAsBytes(new BienFormDTO()));

        mockMvc.perform(multipart("/biens").file(bienPart))
                .andExpect(status().isBadRequest());

        verify(bienService, never()).createBien(any(), any(), any());
    }

    @Test
    @DisplayName("J3 : POST /biens — devise inconnue ou montant hors NUMERIC(12,2) (1e2147483647) : 400, aucun bien créé")
    @WithMockUser(username = "proprietaire@test.com", roles = "PROPRIETAIRE")
    void createBien_deviseOuMontantInvalide_400() throws Exception {
        java.util.Map<String, com.fasterxml.jackson.databind.JsonNode> champs = java.util.Map.of(
                "devise", objectMapper.getNodeFactory().textNode("XXX"),
                // montants énormes en BigDecimal exact (« 1E+2147483647 »), pas en double (Infinity → JSON invalide)
                "loyerMensuel", objectMapper.getNodeFactory().numberNode(new BigDecimal("1e2147483647")),
                "depotGarantie", objectMapper.getNodeFactory().numberNode(new BigDecimal("1e200000")));
        for (var champ : champs.entrySet()) {
            com.fasterxml.jackson.databind.node.ObjectNode noeud = objectMapper.valueToTree(bienFormValide());
            noeud.set(champ.getKey(), champ.getValue());
            String json = objectMapper.writeValueAsString(noeud);
            MockMultipartFile bienPart = new MockMultipartFile(
                    "bienFormDTO", "", "application/json", json.getBytes(java.nio.charset.StandardCharsets.UTF_8));

            mockMvc.perform(multipart("/biens").file(bienPart))
                    .andExpect(status().isBadRequest());
        }
        verify(bienService, never()).createBien(any(), any(), any());
    }

    @Test
    @DisplayName("J1 : POST /biens — pays en texte libre (« France ») ou code inconnu : 400 ; code ISO accepté")
    @WithMockUser(username = "proprietaire@test.com", roles = "PROPRIETAIRE")
    void createBien_paysCodeIso() throws Exception {
        for (String pays : new String[]{"France", "DE", ""}) {
            String json = objectMapper.writeValueAsString(bienFormValide())
                    .replace("\"pays\":\"FR\"", "\"pays\":\"" + pays + "\"");
            MockMultipartFile bienPart = new MockMultipartFile(
                    "bienFormDTO", "", "application/json", json.getBytes(java.nio.charset.StandardCharsets.UTF_8));

            mockMvc.perform(multipart("/biens").file(bienPart))
                    .andExpect(status().isBadRequest());
        }
        verify(bienService, never()).createBien(any(), any(), any());

        BienFormDTO dto = bienFormValide();
        dto.setPays(Pays.CD);
        MockMultipartFile bienPart = new MockMultipartFile(
                "bienFormDTO", "", "application/json", objectMapper.writeValueAsBytes(dto));
        mockMvc.perform(multipart("/biens").file(bienPart))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("POST /biens — adresse contenant « < » refusée : 400, aucun bien créé (C3)")
    @WithMockUser(username = "proprietaire@test.com", roles = "PROPRIETAIRE")
    void createBien_adresseAvecChevron_shouldReturn400() throws Exception {
        BienFormDTO dto = bienFormValide();
        dto.setAdresse("12 rue <script>alert(1)</script>");

        MockMultipartFile bienPart = new MockMultipartFile(
                "bienFormDTO", "", "application/json", objectMapper.writeValueAsBytes(dto));

        mockMvc.perform(multipart("/biens").file(bienPart))
                .andExpect(status().isBadRequest());

        verify(bienService, never()).createBien(any(), any(), any());
    }

    @Test
    @DisplayName("POST /biens — adresse congolaise « N° 12, Av. Kasa-Vubu, Q/Matonge » acceptée (C3)")
    @WithMockUser(username = "proprietaire@test.com", roles = "PROPRIETAIRE")
    void createBien_adresseCongolaise_shouldReturn204() throws Exception {
        BienFormDTO dto = bienFormValide();
        dto.setAdresse("N° 12, Av. Kasa-Vubu, Q/Matonge, C/Kalamu #3 & 4");

        MockMultipartFile bienPart = new MockMultipartFile(
                "bienFormDTO", "", "application/json", objectMapper.writeValueAsBytes(dto));

        mockMvc.perform(multipart("/biens").file(bienPart))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("POST /biens — sans token : 401 (P0-1)")
    void createBien_withoutToken_shouldReturn401() throws Exception {
        MockMultipartFile bienPart = new MockMultipartFile(
                "bienFormDTO", "", "application/json",
                objectMapper.writeValueAsBytes(bienFormValide()));

        mockMvc.perform(multipart("/biens").file(bienPart))
                .andExpect(status().isUnauthorized());

        verify(bienService, never()).createBien(any(), any(), any());
    }

    @Test
    @DisplayName("POST /biens — locataire : 403 (P0-1)")
    @WithMockUser(username = "locataire@test.com", roles = "LOCATAIRE")
    void createBien_asLocataire_shouldReturn403() throws Exception {
        MockMultipartFile bienPart = new MockMultipartFile(
                "bienFormDTO", "", "application/json",
                objectMapper.writeValueAsBytes(bienFormValide()));

        mockMvc.perform(multipart("/biens").file(bienPart))
                .andExpect(status().isForbidden())
                // EXC : corps ApiErrorResponse, pas une 500
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Accès refusé"));

        verify(bienService, never()).createBien(any(), any(), any());
    }

    @Test
    @DisplayName("POST /biens/{id}/assigne-locataire — sans token : 401 (P0-1)")
    void assignLocataire_withoutToken_shouldReturn401() throws Exception {
        mockMvc.perform(post("/biens/1/assigne-locataire/2"))
                .andExpect(status().isUnauthorized());

        verify(bienService, never()).affectLocataire(any(), any(), any());
    }

    @Test
    @DisplayName("GET /biens/{id} — public : accessible sans token (P0-1)")
    void getBienInfos_withoutToken_shouldReturn200() throws Exception {
        when(bienService.getBienInfos(1L)).thenReturn(BienPublicDTO.builder().id(1L).build());

        mockMvc.perform(get("/biens/1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /biens/search — public : accessible sans token (P0-1)")
    void rechercher_withoutToken_shouldNotReturn401() throws Exception {
        mockMvc.perform(post("/biens/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(result -> org.assertj.core.api.Assertions
                        .assertThat(result.getResponse().getStatus()).isNotIn(401, 403));
    }

    // ─────────────────────────────────────────────────────────────
    // GET /biens/{id}
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /biens/{id} — succès : retourne le bien (200)")
    @WithMockUser(username = "user@test.com")
    void getBienInfos_success_shouldReturn200() throws Exception {
        BienPublicDTO dto = BienPublicDTO.builder()
                .id(1L)
                .titre("Appartement T3")
                .typeBien(TypeBien.APPARTEMENT)
                .ville("Nantes")
                .proprietaire(new ProprietairePublicDTO("Jean", "D.", null))
                .build();

        when(bienService.getBienInfos(1L)).thenReturn(dto);

        mockMvc.perform(get("/biens/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.titre").value("Appartement T3"))
                // P0-6 : aucune donnée personnelle dans la vue publique
                .andExpect(jsonPath("$.proprietaire.initialeNom").value("D."))
                .andExpect(jsonPath("$.proprietaire.mail").doesNotExist())
                .andExpect(jsonPath("$.locataire").doesNotExist())
                .andExpect(jsonPath("$.contrats").doesNotExist())
                .andExpect(jsonPath("$.quittances").doesNotExist());
    }

    @Test
    @DisplayName("GET /biens/abc — identifiant non numérique : 400 et non 500 (EXC)")
    void getBienInfos_idNonNumerique_shouldReturn400() throws Exception {
        mockMvc.perform(get("/biens/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("GET /biens/{id} — erreur technique : 500 générique, aucun détail interne ni stacktrace (EXC)")
    void getBienInfos_erreurTechnique_shouldReturn500Generique() throws Exception {
        when(bienService.getBienInfos(1L))
                .thenThrow(new RuntimeException("org.postgresql.util.PSQLException: relation \"bien\" does not exist"));

        mockMvc.perform(get("/biens/1"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Une erreur interne est survenue"))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("PSQLException"))))
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    @Test
    @DisplayName("GET /biens/{id} — exception métier : statut et message métier conservés (priorité de GlobalExceptionHandler)")
    void getBienInfos_exceptionMetier_resteMetier() throws Exception {
        when(bienService.getBienInfos(1L))
                .thenThrow(new KupangaBusinessException("Bien introuvable", HttpStatus.NOT_FOUND));

        mockMvc.perform(get("/biens/1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Bien introuvable"));
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

    @Test
    @DisplayName("POST /biens/search — pagination hors bornes (size=0, size=10000, page=-1) : 400, aucune recherche (VALID)")
    void rechercher_paginationHorsBornes_shouldReturn400() throws Exception {
        for (String body : List.of("{\"size\": 0}", "{\"size\": 10000}", "{\"page\": -1}")) {
            mockMvc.perform(post("/biens/search")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }

        verify(bienSearchService, never()).rechercher(any());
    }

    @Test
    @DisplayName("J3 : POST /biens/search — loyer énorme (1e200000), négatif ou hors NUMERIC(12,2) : 400 et non 500, aucune recherche")
    void rechercher_loyerHorsBornes_shouldReturn400() throws Exception {
        for (String body : List.of("{\"loyerMax\": 1e200000}", "{\"loyerMin\": 1e2147483647}",
                "{\"loyerMin\": -1}", "{\"loyerMax\": 10000000000}")) {
            mockMvc.perform(post("/biens/search")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }

        verify(bienSearchService, never()).rechercher(any());
    }

    @Test
    @DisplayName("POST /biens/search — filtre texte trop long : 400 (VALID)")
    void rechercher_titreTropLong_shouldReturn400() throws Exception {
        mockMvc.perform(post("/biens/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titre\": \"" + "a".repeat(101) + "\"}"))
                .andExpect(status().isBadRequest());

        verify(bienSearchService, never()).rechercher(any());
    }

    // ─────────────────────────────────────────────────────────────
    // PATCH /biens/{id}
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("PATCH /biens/{id} — succès : retourne bien mis à jour (200)")
    @WithMockUser(username = "proprietaire@test.com", roles = "PROPRIETAIRE")
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
    @WithMockUser(username = "proprietaire@test.com", roles = "PROPRIETAIRE")
    void assignLocataire_success_shouldReturn204() throws Exception {
        doNothing().when(bienService).affectLocataire(any(), eq(1L), eq(2L));

        mockMvc.perform(post("/biens/1/assigne-locataire/2"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("POST /biens/{bienId}/assigne-locataire/{userId} — bien introuvable : 404")
    @WithMockUser(username = "proprietaire@test.com", roles = "PROPRIETAIRE")
    void assignLocataire_notFound_shouldReturn404() throws Exception {
        doThrow(new KupangaBusinessException("Bien introuvable", HttpStatus.NOT_FOUND))
                .when(bienService).affectLocataire(any(), eq(99L), eq(2L));

        mockMvc.perform(post("/biens/99/assigne-locataire/2"))
                .andExpect(status().isNotFound());
    }

    private BienFormDTO bienFormValide() {
        return BienFormDTO.builder()
                .titre("Appartement T3")
                .typeBien(TypeBien.APPARTEMENT)
                .adresse("12 rue des Tests")
                .ville("Nantes")
                .codePostal("44000")
                .pays(Pays.FR)
                .surfaceHabitable(65.0)
                .nombrePieces(3)
                .loyerMensuel(new BigDecimal("850.0"))
                .chargesMensuelles(new BigDecimal("50.0"))
                .depotGarantie(new BigDecimal("1700.0"))
                .meuble(false)
                .colocation(false)
                .disponibleDe(java.time.LocalDate.of(2030, 1, 1))
                .build();
    }
}
