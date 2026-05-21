package com.kupanga.api.notification.controller;

import com.kupanga.api.authentification.service.impl.UserDetailsServiceImpl;
import com.kupanga.api.authentification.utils.JwtUtils;
import com.kupanga.api.config.SecurityConfig;
import com.kupanga.api.exception.business.KupangaBusinessException;
import com.kupanga.api.notification.dto.AppNotificationDTO;
import com.kupanga.api.notification.enums.NotificationType;
import com.kupanga.api.notification.service.NotificationService;
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

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(NotificationController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = true)
@DisplayName("Tests NotificationController")
class NotificationControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean private NotificationService     notificationService;
    @MockBean private JwtUtils                jwtUtils;
    @MockBean private UserDetailsServiceImpl  userDetailsService;
    @MockBean private EntityManagerFactory    entityManagerFactory;

    private AppNotificationDTO buildDTO(Long id, NotificationType type) {
        return new AppNotificationDTO(
                id, type,
                "Titre test", "Message test",
                false, "token-xyz", 1L,
                LocalDateTime.of(2026, 5, 1, 10, 0)
        );
    }

    // ─────────────────────────────────────────────────────────────
    // GET /notifications
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /notifications — succès : retourne liste des notifications non lues (200)")
    @WithMockUser(username = "locataire@test.com")
    void getNonLues_authenticated_shouldReturn200() throws Exception {
        List<AppNotificationDTO> notifs = List.of(
                buildDTO(1L, NotificationType.INVITATION_SIGNATURE_CONTRAT),
                buildDTO(2L, NotificationType.CONTRAT_SIGNE)
        );

        when(notificationService.getNonLues("locataire@test.com")).thenReturn(notifs);

        mockMvc.perform(get("/notifications")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].type").value("INVITATION_SIGNATURE_CONTRAT"))
                .andExpect(jsonPath("$[0].lue").value(false))
                .andExpect(jsonPath("$[1].id").value(2));
    }

    @Test
    @DisplayName("GET /notifications — liste vide si aucune notification non lue (200)")
    @WithMockUser(username = "locataire@test.com")
    void getNonLues_empty_shouldReturn200WithEmptyList() throws Exception {
        when(notificationService.getNonLues("locataire@test.com")).thenReturn(List.of());

        mockMvc.perform(get("/notifications")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ─────────────────────────────────────────────────────────────
    // PATCH /notifications/{id}/lire
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("PATCH /notifications/1/lire — succès : notification marquée lue (204)")
    @WithMockUser(username = "locataire@test.com")
    void marquerLue_success_shouldReturn204() throws Exception {
        doNothing().when(notificationService).marquerLue(1L, "locataire@test.com");

        mockMvc.perform(patch("/notifications/1/lire"))
                .andExpect(status().isNoContent());

        verify(notificationService).marquerLue(1L, "locataire@test.com");
    }

    @Test
    @DisplayName("PATCH /notifications/99/lire — notification introuvable → 404")
    @WithMockUser(username = "locataire@test.com")
    void marquerLue_notFound_shouldReturn404() throws Exception {
        doThrow(new KupangaBusinessException("Notification introuvable", HttpStatus.NOT_FOUND))
                .when(notificationService).marquerLue(99L, "locataire@test.com");

        mockMvc.perform(patch("/notifications/99/lire"))
                .andExpect(status().isNotFound());
    }

    // ─────────────────────────────────────────────────────────────
    // PATCH /notifications/lire-toutes
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("PATCH /notifications/lire-toutes — succès : toutes marquées lues (204)")
    @WithMockUser(username = "locataire@test.com")
    void marquerToutesLues_success_shouldReturn204() throws Exception {
        doNothing().when(notificationService).marquerToutesLues("locataire@test.com");

        mockMvc.perform(patch("/notifications/lire-toutes"))
                .andExpect(status().isNoContent());

        verify(notificationService).marquerToutesLues("locataire@test.com");
    }
}
