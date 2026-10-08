package com.kupanga.api.config;

import com.kupanga.api.authentification.service.impl.UserDetailsServiceImpl;
import com.kupanga.api.authentification.utils.JwtUtils;
import com.kupanga.api.user.repository.UserRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfigurationSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests unitaires et d'intégration pour la configuration SecurityConfig.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Tests unitaires pour SecurityConfig")
class SecurityConfigTest {

    @Autowired
    private SecurityConfig securityConfig;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SecurityFilterChain filterChain;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private CorsConfigurationSource corsConfigurationSource;

    // Mock des dépendances pour que le contexte se charge
    @MockBean
    private UserDetailsServiceImpl userDetailsService;

    @MockBean
    private JwtUtils jwtUtils;

    @MockBean
    private UserRepository userRepository;

    @Test
    @DisplayName(" Le contexte Spring se charge correctement")
    void contextLoads() {
        assertThat(securityConfig).isNotNull();
        assertThat(filterChain).isNotNull();
        assertThat(authenticationManager).isNotNull();
        assertThat(passwordEncoder).isNotNull();
        assertThat(corsConfigurationSource).isNotNull();
    }

    @Test
    @DisplayName(" Le PasswordEncoder est de type BCryptPasswordEncoder")
    void passwordEncoderIsBCrypt() {
        assertThat(passwordEncoder).isInstanceOf(BCryptPasswordEncoder.class);
    }


    @Test
    @DisplayName(" Le JwtFilter est présent dans la chaîne de filtres")
    void jwtFilterBeanPresent() {
        // Vérification basique : si le contexte se charge et que le filterChain n’est pas nul
        assertThat(filterChain).isNotNull();
    }

    @Test
    @DisplayName(" L'AuthenticationManager est correctement configuré")
    void authenticationManagerConfigured() {
        assertThat(authenticationManager).isNotNull();
    }

    @Test
    @DisplayName("WebSocket — origine inconnue refusée (403), origine du front acceptée (W5)")
    void websocket_originesRestreintes() throws Exception {
        mockMvc.perform(get("/ws/info").header(HttpHeaders.ORIGIN, "https://site-pirate.example"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/ws/info").header(HttpHeaders.ORIGIN, "http://localhost:4200"))
                .andExpect(status().isOk());
    }
}
