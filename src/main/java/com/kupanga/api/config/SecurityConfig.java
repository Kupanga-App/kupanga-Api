package com.kupanga.api.config;

import com.kupanga.api.authentification.filter.JwtFilter;
import com.kupanga.api.authentification.service.impl.UserDetailsServiceImpl;
import com.kupanga.api.authentification.utils.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Configuration de la sécurité de l'application.
 * Configure l'authentification, les filtres JWT, la gestion CORS et les règles d'accès aux endpoints.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties(CorsProperties.class)
@RequiredArgsConstructor
public class SecurityConfig {

    /** Service de gestion des utilisateurs pour Spring Security */
    private final UserDetailsServiceImpl userDetailsService;

    /** Utilitaire pour manipuler les JWT */
    private final JwtUtils jwtUtils;

    /** Origines autorisées (A11) */
    private final CorsProperties corsProperties;

    /**
     * Bean pour encoder les mots de passe avec BCrypt.
     * @return un PasswordEncoder utilisant BCrypt
     */
    @Bean
    public PasswordEncoder passwordEncoder() {

        return new BCryptPasswordEncoder();
    }

    /**
     * Bean pour gérer l'authentification.
     *
     * @param httpSecurity HttpSecurity fourni par Spring
     * @param passwordEncoder PasswordEncoder pour valider les mots de passe
     * @return AuthenticationManager configuré
     * @throws Exception en cas d'erreur de configuration
     */
    @Bean
    public AuthenticationManager authenticationManager(
            HttpSecurity httpSecurity,
            PasswordEncoder passwordEncoder
    ) throws Exception {

        AuthenticationManagerBuilder authBuilder = httpSecurity.getSharedObject(AuthenticationManagerBuilder.class);
        authBuilder.userDetailsService(userDetailsService).passwordEncoder(passwordEncoder);
        return authBuilder.build();
    }

    /**
     * Bean définissant la chaîne de filtres de sécurité.
     * Configure CORS, CSRF, les règles d'accès et ajoute le filtre JWT avant
     * UsernamePasswordAuthenticationFilter.
     * @param http HttpSecurity fourni par Spring
     * @return SecurityFilterChain configurée
     * @throws Exception en cas d'erreur de configuration
     */
    @Bean
    @Order(2)
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
                //  CORS configuré via le bean corsConfigurationSource()
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                //  CSRF désactivé pour API REST
                .csrf(AbstractHttpConfigurer::disable)

                //  Pas de session HTTP — chaque requête est authentifiée via JWT uniquement
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                //  Règles d'autorisation : tout est fermé sauf la liste publique ci-dessous
                .authorizeHttpRequests(auth -> auth
                        // Authentification
                        .requestMatchers(HttpMethod.POST,
                                "/auth/login", "/auth/register", "/auth/google", "/auth/refresh",
                                "/auth/forgot-password", "/auth/reset-password", "/auth/logout").permitAll()
                        // Consultation publique des biens
                        .requestMatchers(HttpMethod.GET, "/biens/*").permitAll()
                        .requestMatchers(HttpMethod.POST, "/biens/search").permitAll()
                        // Signature par le locataire via le lien reçu par e-mail (le token sert d'autorisation)
                        .requestMatchers("/contrats/signer/*", "/etats-des-lieux/signer/*").permitAll()
                        // WebSocket : l'authentification se fait sur la trame STOMP CONNECT (JwtChannelInterceptor)
                        .requestMatchers("/ws/**").permitAll()
                        // Supervision
                        .requestMatchers(HttpMethod.GET, "/health", "/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers("/actuator/**").denyAll()
                        // Swagger : désactivé en prod (springdoc), ouvert en dev/test
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        // Page d'erreur interne de Spring (sinon toute erreur devient 401)
                        .requestMatchers("/error").permitAll()
                        .anyRequest().authenticated()
                )

                //  401 (et non 403 ou redirection) quand aucun utilisateur n'est authentifié
                .exceptionHandling(e -> e.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))

                //  Ajout du filtre JWT avant UsernamePasswordAuthenticationFilter
                .addFilterBefore(
                        new JwtFilter(userDetailsService, jwtUtils),
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }

    /**
     * Bean de configuration CORS : seules les origines de {@code app.cors.allowed-origins}
     * (front Angular) peuvent appeler l'API avec les cookies (A11).
     * @return CorsConfigurationSource configuré
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(corsProperties.allowedOrigins());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
