package com.kupanga.api.juridiction.controller;

import com.kupanga.api.authentification.service.impl.UserDetailsServiceImpl;
import com.kupanga.api.authentification.utils.JwtUtils;
import com.kupanga.api.config.SecurityConfig;
import com.kupanga.api.juridiction.JuridictionRegistry;
import com.kupanga.api.juridiction.JuridictionsDeTest;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * J6 : configuration publique du formulaire de bien par pays, servie depuis les vrais profils d'{@code application.yml}.
 */
@WebMvcTest(JuridictionController.class)
@Import({SecurityConfig.class, JuridictionControllerWebMvcTest.Registre.class})
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = true)
@DisplayName("J6 : GET /juridictions (public, lecture seule)")
class JuridictionControllerWebMvcTest {

    @TestConfiguration
    static class Registre {
        @Bean
        JuridictionRegistry juridictionRegistry() {
            return JuridictionsDeTest.registre();
        }
    }

    @Autowired private MockMvc mockMvc;

    @MockBean private JwtUtils               jwtUtils;
    @MockBean private UserDetailsServiceImpl userDetailsService;
    @MockBean private EntityManagerFactory   entityManagerFactory;

    @Test
    @DisplayName("GET /juridictions sans jeton — pays pris en charge seulement (FR, CD), mis en cache 1 h")
    void paysPrisEnCharge() throws Exception {
        mockMvc.perform(get("/juridictions"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("max-age=3600")))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].pays").value("FR"))
                .andExpect(jsonPath("$[0].libelle").value("France"))
                .andExpect(jsonPath("$[1].pays").value("CD"));
    }

    @Test
    @DisplayName("GET /juridictions/CD sans jeton — devises, champs obligatoires et masqués, types de bien")
    void configurationRdc() throws Exception {
        mockMvc.perform(get("/juridictions/CD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pays").value("CD"))
                .andExpect(jsonPath("$.devises[0]").value("USD"))
                .andExpect(jsonPath("$.devises[1]").value("CDF"))
                .andExpect(jsonPath("$.deviseDefaut").value("USD"))
                .andExpect(jsonPath("$.champsObligatoires[0]").value("commune"))
                .andExpect(jsonPath("$.champsObligatoires[1]").value("quartier"))
                .andExpect(jsonPath("$.champsMasques", hasSize(3)))
                .andExpect(jsonPath("$.typesBien[0]").value("APPARTEMENT"))
                // Données de configuration seulement : ni locale, ni fuseau, ni plafonds, ni modèle de document
                .andExpect(jsonPath("$.modeleDocuments").doesNotExist())
                .andExpect(jsonPath("$.plafonds").doesNotExist());
    }

    @Test
    @DisplayName("GET /juridictions/FR — code postal obligatoire, adresse congolaise masquée")
    void configurationFrance() throws Exception {
        mockMvc.perform(get("/juridictions/FR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.champsObligatoires[0]").value("codePostal"))
                .andExpect(jsonPath("$.champsMasques", hasSize(5)))
                .andExpect(jsonPath("$.devises[0]").value("EUR"));
    }

    @Test
    @DisplayName("Pays sans profil (BE) → 404 ; code inconnu (XX, france) → 400")
    void paysNonPrisEnChargeOuInconnu() throws Exception {
        mockMvc.perform(get("/juridictions/BE")).andExpect(status().isNotFound());
        mockMvc.perform(get("/juridictions/XX")).andExpect(status().isBadRequest());
        mockMvc.perform(get("/juridictions/france")).andExpect(status().isBadRequest());
    }
}
