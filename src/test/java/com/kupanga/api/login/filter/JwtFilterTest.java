package com.kupanga.api.login.filter;

import com.kupanga.api.login.service.UserDetailsServiceImpl;
import com.kupanga.api.login.utils.JwtUtils;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
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
    @DisplayName("Token valide : l'utilisateur est authentifié")
    void shouldAuthenticateUserWithValidToken() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer validToken");
        when(jwtUtils.extractUserEmail("validToken")).thenReturn("test@mail.com");
        when(userDetailsService.loadUserByUsername("test@mail.com")).thenReturn(userDetails);
        when(jwtUtils.validateToken("validToken", userDetails)).thenReturn(true);

        jwtFilter.doFilterInternal(request, response, filterChain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        UsernamePasswordAuthenticationToken auth =
                (UsernamePasswordAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();

        assertEquals("test@mail.com", auth.getName());

        // Vérifie que le rôle ROLE_LOCATAIRE est présent
        assertTrue(auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_LOCATAIRE")));

        verify(filterChain, times(1)).doFilter(request, response);
    }


    @Test
    @DisplayName("Token invalide : l'utilisateur n'est pas authentifié")
    void shouldNotAuthenticateWithInvalidToken() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer invalidToken");
        when(jwtUtils.extractUserEmail("invalidToken")).thenThrow(new RuntimeException("Token invalide"));

        jwtFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    @DisplayName("Token expiré : l'utilisateur n'est pas authentifié")
    void shouldNotAuthenticateWithExpiredToken() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer expiredToken");
        when(jwtUtils.extractUserEmail("expiredToken")).thenThrow(new ExpiredJwtException(null, null, "Token expiré"));

        jwtFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    @DisplayName("JWT dans cookie : l'utilisateur est authentifié")
    void shouldAuthenticateUserFromCookie() throws Exception {
        Cookie jwtCookie = new Cookie("jwt_token", "cookieToken");
        when(request.getHeader("Authorization")).thenReturn(null);
        when(request.getCookies()).thenReturn(new Cookie[]{jwtCookie});
        when(jwtUtils.extractUserEmail("cookieToken")).thenReturn("test@mail.com");
        when(userDetailsService.loadUserByUsername("test@mail.com")).thenReturn(userDetails);
        when(jwtUtils.validateToken("cookieToken", userDetails)).thenReturn(true);

        jwtFilter.doFilterInternal(request, response, filterChain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        UsernamePasswordAuthenticationToken auth =
                (UsernamePasswordAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        assertEquals("test@mail.com", auth.getName());
    }
}

