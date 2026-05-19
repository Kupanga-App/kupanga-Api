package com.kupanga.api.config;

import com.kupanga.api.authentification.service.impl.UserDetailsServiceImpl;
import com.kupanga.api.authentification.utils.JwtUtils;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(HealthController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = true)
@DisplayName("Tests HealthController")
class HealthControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean private JdbcTemplate           jdbcTemplate;
    @MockBean private JwtUtils               jwtUtils;
    @MockBean private UserDetailsServiceImpl  userDetailsService;
    @MockBean private EntityManagerFactory   entityManagerFactory;

    // ─────────────────────────────────────────────────────────────
    // GET /health
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /health — succès : base de données joignable (200)")
    void health_dbReachable_shouldReturn200() throws Exception {
        doNothing().when(jdbcTemplate).execute(anyString());

        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(content().string("OK : render et neon reveillé"));
    }
}
