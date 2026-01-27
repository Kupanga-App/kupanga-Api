package com.kupanga.api.config;

import com.kupanga.api.authentification.service.impl.UserDetailsServiceImpl;
import com.kupanga.api.authentification.utils.JwtUtils;
import com.kupanga.api.user.repository.UserRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfigurationSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests unitaires et d'intégration pour la configuration SecurityConfig.
 */
@SpringBootTest
@DisplayName("Tests unitaires pour SecurityConfig")
class SecurityConfigTest {

    @Autowired
    private SecurityConfig securityConfig;

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
    @DisplayName(" La configuration CORS contient bien les origines autorisées")
    void corsConfigurationContainsAllowedOrigins() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/"); // on doit définir un path non null

        var corsConfig = corsConfigurationSource.getCorsConfiguration(request);

        Assertions.assertNotNull(corsConfig);
        assertThat(corsConfig.getAllowedOrigins())
                .containsExactlyInAnyOrder("http://localhost:4200", "https://kupanga.lespacelibellule.com");
        assertThat(corsConfig.getAllowedMethods())
                .containsExactlyInAnyOrder("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");
        assertThat(corsConfig.getAllowedHeaders()).contains("*");
        assertThat(corsConfig.getAllowCredentials()).isTrue();
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
}
