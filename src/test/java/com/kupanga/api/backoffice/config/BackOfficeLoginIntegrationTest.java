package com.kupanga.api.backoffice.config;

import com.kupanga.api.authentification.utils.JwtUtils;
import com.kupanga.api.user.entity.Role;
import com.kupanga.api.user.entity.User;
import com.kupanga.api.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * BO-LOGIN : login du back-office (formLogin), avec la vraie chaîne de sécurité.
 * Chaque test utilise sa propre IP : les compteurs de la limite de tentatives sont partagés dans le contexte.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@Transactional
@DisplayName("Tests d'intégration — login du back-office")
class BackOfficeLoginIntegrationTest {

    private static final String ADMIN_EMAIL = "test-admin@kupanga.test";
    private static final String ADMIN_PASSWORD = "test-admin-password";

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtUtils jwtUtils;

    @Test
    @DisplayName("Identifiants admin corrects → dashboard")
    void admin_connecte() throws Exception {
        login(ADMIN_EMAIL, ADMIN_PASSWORD, "198.51.100.1")
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/backoffice/dashboard"));
    }

    @Test
    @DisplayName("E-mail admin insensible à la casse")
    void admin_emailEnMajuscules_connecte() throws Exception {
        login(ADMIN_EMAIL.toUpperCase(), ADMIN_PASSWORD, "198.51.100.2")
                .andExpect(redirectedUrl("/backoffice/dashboard"));
    }

    @Test
    @DisplayName("Mauvais mot de passe admin → ?error=true")
    void admin_mauvaisMotDePasse_refuse() throws Exception {
        login(ADMIN_EMAIL, "mauvais", "198.51.100.3")
                .andExpect(redirectedUrl("/backoffice/login?error=true"));
    }

    @Test
    @DisplayName("Un utilisateur de l'app avec ses vrais identifiants n'entre pas dans le back-office")
    void utilisateurApp_refuse() throws Exception {
        userRepository.save(User.builder()
                .firstName("Paul").lastName("Proprio").mail("proprio@bo.test")
                .password(passwordEncoder.encode("MotDePasse123!"))
                .role(Role.ROLE_PROPRIETAIRE).hasCompleteProfil(true)
                .build());

        login("proprio@bo.test", "MotDePasse123!", "198.51.100.4")
                .andExpect(redirectedUrl("/backoffice/login?error=true"));
    }

    @Test
    @DisplayName("6e essai depuis la même IP → ?bloque=true, même avec le bon mot de passe")
    void sixiemeEssai_bloque() throws Exception {
        String ip = "198.51.100.5";
        for (int i = 0; i < 5; i++) {
            login(ADMIN_EMAIL, "mauvais-" + i, ip)
                    .andExpect(redirectedUrl("/backoffice/login?error=true"));
        }

        login(ADMIN_EMAIL, ADMIN_PASSWORD, ip)
                .andExpect(redirectedUrl("/backoffice/login?bloque=true"));

        // Une autre IP n'est pas bloquée
        login(ADMIN_EMAIL, ADMIN_PASSWORD, "198.51.100.6")
                .andExpect(redirectedUrl("/backoffice/dashboard"));
    }

    @Test
    @DisplayName("Identifiants vides → ?error=true, pas d'erreur 500")
    void identifiantsVides_refuse() throws Exception {
        login("", "", "198.51.100.7")
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/backoffice/login?error=true"));
    }

    @Test
    @DisplayName("Un JWT valide de l'API ne donne pas accès au back-office (chaînes isolées)")
    void jwtValide_neDonnePasAccesAuBackOffice() throws Exception {
        String jwt = jwtUtils.generateAccessToken("proprio@bo.test", Role.ROLE_PROPRIETAIRE.name());

        mockMvc.perform(get("/backoffice/dashboard").header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/backoffice/login"));
    }

    @Test
    @WithMockUser(authorities = "ROLE_PROPRIETAIRE")
    @DisplayName("Authentifié sans l'autorité admin → 403 sur le back-office")
    void authentifieSansAutoriteAdmin_refuse() throws Exception {
        mockMvc.perform(get("/backoffice/dashboard"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST multipart sans session sur le back-office → 415, corps non lu (B3)")
    void multipart_refuse() throws Exception {
        mockMvc.perform(post("/backoffice/biens/1/supprimer")
                        .contentType("multipart/form-data; boundary=x")
                        .content("--x--"))
                .andExpect(status().isUnsupportedMediaType());
    }

    private ResultActions login(String email, String password, String ip)
            throws Exception {
        return mockMvc.perform(post("/backoffice/login")
                .param("username", email)
                .param("password", password)
                .with(csrf())
                .with(depuis(ip)));
    }

    private static RequestPostProcessor depuis(String ip) {
        return request -> {
            request.setRemoteAddr(ip);
            return request;
        };
    }
}
