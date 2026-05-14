package com.kupanga.api.backoffice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AdminCredentialsAuthProvider implements AuthenticationProvider {

    @Value("${ADMIN_EMAIL}")
    private String adminEmail;

    @Value("${ADMIN_PASSWORD}")
    private String adminPassword;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String email    = authentication.getName();
        String password = authentication.getCredentials().toString();

        if (adminEmail.equalsIgnoreCase(email) && adminPassword.equals(password)) {
            return new UsernamePasswordAuthenticationToken(
                    email,
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_BACKOFFICE_ADMIN"))
            );
        }
        throw new BadCredentialsException("Identifiants invalides");
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
