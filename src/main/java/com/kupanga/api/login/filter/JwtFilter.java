package com.kupanga.api.login.filter;

import com.kupanga.api.login.service.UserDetailsServiceImpl;
import com.kupanga.api.login.utils.JwtUtils;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filtre JWT exécuté une seule fois par requête.
 *
 * <p>
 * Récupère le token JWT soit depuis le header "Authorization: Bearer <token>",
 * soit depuis le cookie HttpOnly "jwt_token".
 * Si le token est valide, l'utilisateur est authentifié dans Spring Security.
 * </p>
 */
@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final UserDetailsServiceImpl userDetailsService;
    private final JwtUtils jwtUtils;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    @NotNull HttpServletResponse response,
                                    @NotNull FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        String jwt = null;
        String userEmail = null;

        // Récupération du JWT depuis le header "Authorization" ou cookie "jwt_token"
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            jwt = authHeader.substring(7);
        } else if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("jwt_token".equals(cookie.getName())) {
                    jwt = cookie.getValue();
                    break;
                }
            }
        }

        // Extraction de l'email depuis le JWT
        if (jwt != null) {
            try {
                userEmail = jwtUtils.extractUserEmail(jwt);
            } catch (ExpiredJwtException e) {
                SecurityContextHolder.clearContext();
                logger.warn("JWT expiré : {}");
            } catch (Exception e) {
                SecurityContextHolder.clearContext();
                logger.warn("JWT invalide : {}");
            }
        }

        // Authentification si email présent et pas déjà authentifié
        if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);

                if (jwtUtils.validateToken(jwt, userDetails)) {
                    UsernamePasswordAuthenticationToken authenticationToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities()
                            );

                    authenticationToken.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request)
                    );

                    SecurityContextHolder.getContext().setAuthentication(authenticationToken);
                }
            } catch (Exception e) {
                SecurityContextHolder.clearContext();
                logger.warn("Impossible d'authentifier l'utilisateur depuis le JWT : {}");
            }
        }

        filterChain.doFilter(request, response);
    }
}
