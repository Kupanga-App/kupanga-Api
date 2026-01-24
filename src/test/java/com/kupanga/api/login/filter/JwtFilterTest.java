package com.kupanga.api.login.filter;

import com.kupanga.api.login.service.impl.UserDetailsServiceImpl;
import com.kupanga.api.login.utils.JwtUtils;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests unitaires pour JwtFilter avec nouvelle implémentation")
class JwtFilterTest {

    @Mock
    private UserDetailsServiceImpl userDetailsService;

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtFilter jwtFilter;

    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        userDetails = new User(
                "test@mail.com",
                "encodedPassword",
                Collections.singleton(() -> "ROLE_LOCATAIRE")
        );
    }

    @Test
    @DisplayName("Token valide : utilisateur authentifié et context Spring rempli")
    void shouldAuthenticateUserWithValidToken() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer validToken");
        when(jwtUtils.extractUserEmail("validToken")).thenReturn("test@mail.com");
        when(userDetailsService.loadUserByUsername("test@mail.com")).thenReturn(userDetails);
        when(jwtUtils.validateToken("validToken", userDetails)).thenReturn(true);

        jwtFilter.doFilterInternal(request, response, filterChain);

        UsernamePasswordAuthenticationToken auth =
                (UsernamePasswordAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertEquals("test@mail.com", auth.getName());
        assertTrue(auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_LOCATAIRE")));

        verify(filterChain, times(1)).doFilter(request, response);
        verify(response, never()).sendError(anyInt(), anyString());
    }

    @Test
    @DisplayName("Token invalide : renvoie 401 et ne remplit pas le context")
    void shouldNotAuthenticateWithInvalidToken() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer invalidToken");
        when(jwtUtils.extractUserEmail("invalidToken")).thenThrow(new RuntimeException("Token invalide"));

        jwtFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(response, times(1)).sendError(HttpServletResponse.SC_UNAUTHORIZED, "Impossible d'authentifier");
        verify(filterChain, never()).doFilter(request, response); // ne continue pas
    }

    @Test
    @DisplayName("Token expiré : renvoie 401 et ne remplit pas le context")
    void shouldNotAuthenticateWithExpiredToken() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer expiredToken");
        when(jwtUtils.extractUserEmail("expiredToken"))
                .thenThrow(new ExpiredJwtException(null, null, "Token expiré"));

        jwtFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(response, times(1)).sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token expiré");
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    @DisplayName("Pas de token : continue la chaîne sans erreur")
    void shouldContinueChainWithoutToken() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(null);

        jwtFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain, times(1)).doFilter(request, response);
        verify(response, never()).sendError(anyInt(), anyString());
    }
}
