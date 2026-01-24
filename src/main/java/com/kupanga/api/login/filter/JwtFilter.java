package com.kupanga.api.login.filter;

import com.kupanga.api.login.service.impl.UserDetailsServiceImpl;
import com.kupanga.api.login.utils.JwtUtils;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
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

        final String authHeader = request.getHeader("Authorization");
        String jwt = null;
        String userEmail = null;

        // 1️. Extraction du JWT depuis le header Authorization
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            jwt = authHeader.substring(7);
        }

        // 2️. Si token présent, extraction de l’email et validation
        if (jwt != null) {
            try {
                userEmail = jwtUtils.extractUserEmail(jwt);

                if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                    // Charger l’utilisateur depuis la DB
                    UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);

                    // Vérifier la validité du token
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

                        // Mettre à jour le contexte Spring Security
                        SecurityContextHolder.getContext().setAuthentication(authenticationToken);
                    } else {
                        // Token invalide
                        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token invalide");
                        return;
                    }
                }

            } catch (ExpiredJwtException e) {
                // Token expiré
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token expiré");
                return;
            } catch (Exception e) {
                // Autres erreurs liées au JWT
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Impossible d'authentifier");
                return;
            }
        }

        // 3️. Continuer la chaîne de filtres
        filterChain.doFilter(request, response);
    }
}
