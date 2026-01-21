package com.kupanga.api.config;

import com.kupanga.api.login.filter.JwtFilter;
import com.kupanga.api.login.service.impl.UserDetailsServiceImpl;
import com.kupanga.api.login.utils.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Configuration de la sécurité de l'application.
 * <p>
 * Configure l'authentification, les filtres JWT, la gestion CORS et les règles d'accès aux endpoints.
 * </p>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    /** Service de gestion des utilisateurs pour Spring Security */
    private final UserDetailsServiceImpl userDetailsService;

    /** Utilitaire pour manipuler les JWT */
    private final JwtUtils jwtUtils;

    /**
     * Bean pour encoder les mots de passe avec BCrypt.
     *
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
     * <p>
     * Configure CORS, CSRF, les règles d'accès et ajoute le filtre JWT avant
     * UsernamePasswordAuthenticationFilter.
     * </p>
     *
     * @param http HttpSecurity fourni par Spring
     * @return SecurityFilterChain configurée
     * @throws Exception en cas d'erreur de configuration
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
                // ⚡ CORS configuré via le bean corsConfigurationSource()
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // ⚡ CSRF désactivé pour API REST
                .csrf(AbstractHttpConfigurer::disable)

                // 🔐 Règles d'autorisation
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/public/**").permitAll() // Endpoints publics
                        .anyRequest().authenticated()             // Tout le reste nécessite authentification
                )

                // ⚡ Ajout du filtre JWT avant UsernamePasswordAuthenticationFilter
                .addFilterBefore(
                        new JwtFilter(userDetailsService, jwtUtils),
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }

    /**
     * Bean de configuration CORS pour autoriser Angular (localhost:4200)
     * et le domaine de production.
     *
     * @return CorsConfigurationSource configuré
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(
                "http://localhost:4200",
                "https://kupanga.lespacelibellule.com"
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
