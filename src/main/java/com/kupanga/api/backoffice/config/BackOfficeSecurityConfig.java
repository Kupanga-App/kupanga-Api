package com.kupanga.api.backoffice.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@RequiredArgsConstructor
public class BackOfficeSecurityConfig {

    private final AdminCredentialsAuthProvider adminCredentialsAuthProvider;

    @Bean
    @Order(1)
    public SecurityFilterChain backOfficeFilterChain(HttpSecurity http) throws Exception {

        http
                // Cette chaîne ne s'applique QUE aux routes /backoffice/**
                .securityMatcher("/backoffice/**")

                // Provider custom : vérifie ADMIN_EMAIL / ADMIN_PASSWORD depuis les variables d'env
                // Aucun accès à la BDD
                .authenticationProvider(adminCredentialsAuthProvider)

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/backoffice/login").permitAll()
                        .anyRequest().authenticated()
                )

                .formLogin(form -> form
                        .loginPage("/backoffice/login")
                        .loginProcessingUrl("/backoffice/login")
                        .defaultSuccessUrl("/backoffice/dashboard", true)
                        .failureUrl("/backoffice/login?error=true")
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
}
