package com.kupanga.api.backoffice.config;

import com.kupanga.api.authentification.ratelimit.LimiteurTentatives;
import com.kupanga.api.exception.business.TropDeTentativesException;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.authentication.WebAuthenticationDetails;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Locale;

import static com.kupanga.api.authentification.ratelimit.LimiteTentatives.BACKOFFICE_LOGIN_GLOBAL;
import static com.kupanga.api.authentification.ratelimit.LimiteTentatives.BACKOFFICE_LOGIN_PAR_IP;

/**
 * Vérifie ADMIN_EMAIL / ADMIN_PASSWORD (aucun accès à la BDD).
 * Volontairement pas un bean Spring : instancié par {@link BackOfficeSecurityConfig} pour son seul manager,
 * afin de n'être jamais enregistré dans le manager global de l'API (BO-LOGIN).
 */
public class AdminCredentialsAuthProvider implements AuthenticationProvider {

    static final String AUTORITE_ADMIN = "ROLE_BACKOFFICE_ADMIN";

    private final String adminEmail;
    private final String adminPassword;
    private final LimiteurTentatives limiteurTentatives;

    public AdminCredentialsAuthProvider(String adminEmail, String adminPassword,
                                        LimiteurTentatives limiteurTentatives) {
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
        this.limiteurTentatives = limiteurTentatives;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        // BO-LOGIN : limite de tentatives avant toute comparaison (5 / 15 min par IP, 100 / h au total)
        try {
            limiteurTentatives.verifierAdresseIp(BACKOFFICE_LOGIN_PAR_IP, adresseIp(authentication));
            limiteurTentatives.verifierGlobal(BACKOFFICE_LOGIN_GLOBAL);
        } catch (TropDeTentativesException e) {
            throw new TropDeTentativesBackOfficeException();
        }

        String email    = authentication.getName() != null ? authentication.getName() : "";
        String password = authentication.getCredentials() != null ? authentication.getCredentials().toString() : "";

        // BO-LOGIN : comparaison en temps constant, les deux évaluées (pas de court-circuit)
        boolean emailOk    = egalEnTempsConstant(adminEmail.toLowerCase(Locale.ROOT), email.toLowerCase(Locale.ROOT));
        boolean passwordOk = egalEnTempsConstant(adminPassword, password);

        if (emailOk & passwordOk) {
            return new UsernamePasswordAuthenticationToken(
                    email,
                    null,
                    List.of(new SimpleGrantedAuthority(AUTORITE_ADMIN))
            );
        }
        throw new BadCredentialsException("Identifiants invalides");
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }

    private static String adresseIp(Authentication authentication) {
        return authentication.getDetails() instanceof WebAuthenticationDetails details
                ? details.getRemoteAddress()
                : null;
    }

    /**
     * Compare les empreintes SHA-256 (longueur fixe) : le temps ne dépend ni du contenu ni de la longueur des chaînes.
     */
    private static boolean egalEnTempsConstant(String attendu, String recu) {
        return MessageDigest.isEqual(sha256(attendu), sha256(recu));
    }

    private static byte[] sha256(String valeur) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(valeur.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponible", e);
        }
    }
}
