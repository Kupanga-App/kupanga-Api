package com.kupanga.api.backoffice.config;

import com.kupanga.api.authentification.ratelimit.LimiteurTentatives;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.ExceptionMappingAuthenticationFailureHandler;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;

@Configuration
public class BackOfficeSecurityConfig {

    @Bean
    @Order(1)
    public SecurityFilterChain backOfficeFilterChain(HttpSecurity http,
                                                     LimiteurTentatives limiteurTentatives,
                                                     @Value("${ADMIN_EMAIL}") String adminEmail,
                                                     @Value("${ADMIN_PASSWORD}") String adminPassword) throws Exception {

        AdminCredentialsAuthProvider adminCredentialsAuthProvider =
                new AdminCredentialsAuthProvider(adminEmail, adminPassword, limiteurTentatives);

        http
                // Cette chaîne ne s'applique QUE aux routes /backoffice/**
                .securityMatcher("/backoffice/**")

                // Provider custom : vérifie ADMIN_EMAIL / ADMIN_PASSWORD depuis les variables d'env
                // Aucun accès à la BDD. Manager dédié, sans parent (BO-LOGIN) : sinon, après un échec, le manager
                // global rappelle le même provider (2 essais consommés par tentative) ou un autre mode de connexion.
                .authenticationManager(new ProviderManager(adminCredentialsAuthProvider))

                // B3 : aucun formulaire du back-office n'envoie de fichier. Sans ce filtre, le CsrfFilter lit
                // _csrf dans les paramètres et fait analyser par Tomcat un corps multipart jusqu'à 50 Mo,
                // même sans session.
                .addFilterBefore(new RefusMultipartFilter(), CsrfFilter.class)

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/backoffice/login").permitAll()
                        // Défense en profondeur (BO-LOGIN) : l'autorité admin, pas seulement « authentifié »
                        .anyRequest().hasAuthority(AdminCredentialsAuthProvider.AUTORITE_ADMIN)
                )

                .formLogin(form -> form
                        .loginPage("/backoffice/login")
                        .loginProcessingUrl("/backoffice/login")
                        .defaultSuccessUrl("/backoffice/dashboard", true)
                        .failureHandler(gestionnaireEchecLogin())
                        .permitAll()
                )

                .logout(logout -> logout
                        .logoutUrl("/backoffice/logout")
                        .logoutSuccessUrl("/backoffice/login?logout=true")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                );

        // CSRF : activé par défaut — protège les formulaires Thymeleaf
        // Session HTTP : activée par défaut (IF_REQUIRED) — gestion transparente par Spring Security
        // Pas de filtre JWT ici — la chaîne API JWT (@Order 2) n'est jamais consultée pour ces routes

        return http.build();
    }

    /** Refuse (415) toute requête multipart sur le back-office, avant que son corps ne soit lu. */
    static class RefusMultipartFilter extends OncePerRequestFilter {
        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
                throws ServletException, IOException {
            String contentType = request.getContentType();
            if (contentType != null && contentType.toLowerCase(Locale.ROOT).startsWith("multipart/")) {
                response.sendError(HttpStatus.UNSUPPORTED_MEDIA_TYPE.value());
                return;
            }
            chain.doFilter(request, response);
        }
    }

    /**
     * Échec de connexion : {@code ?error=true} pour de mauvais identifiants,
     * {@code ?bloque=true} quand la limite de tentatives est atteinte (BO-LOGIN).
     */
    static AuthenticationFailureHandler gestionnaireEchecLogin() {
        ExceptionMappingAuthenticationFailureHandler handler = new ExceptionMappingAuthenticationFailureHandler();
        handler.setDefaultFailureUrl("/backoffice/login?error=true");
        handler.setExceptionMappings(Map.of(
                TropDeTentativesBackOfficeException.class.getName(), "/backoffice/login?bloque=true"
        ));
        return handler;
    }
}
