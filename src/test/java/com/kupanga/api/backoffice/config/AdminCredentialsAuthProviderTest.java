package com.kupanga.api.backoffice.config;

import com.kupanga.api.authentification.ratelimit.LimiteTentatives;
import com.kupanga.api.authentification.ratelimit.LimiteurTentatives;
import com.kupanga.api.authentification.ratelimit.RateLimitConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.WebAuthenticationDetails;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * BO-LOGIN : provider du login back-office, avec un vrai limiteur (stockage mémoire).
 */
@DisplayName("Tests unitaires — AdminCredentialsAuthProvider")
class AdminCredentialsAuthProviderTest {

    private static final String ADMIN_EMAIL = "admin@kupanga.test";
    private static final String ADMIN_PASSWORD = "mot-de-passe-admin";

    private final AdminCredentialsAuthProvider provider = new AdminCredentialsAuthProvider(
            ADMIN_EMAIL, ADMIN_PASSWORD, new LimiteurTentatives(new RateLimitConfig().stockageSeauxMemoire()));

    @Test
    @DisplayName("Identifiants corrects → autorité ROLE_BACKOFFICE_ADMIN, sans mot de passe conservé")
    void identifiantsCorrects_autoriteAdmin() {
        Authentication resultat = provider.authenticate(tentative(ADMIN_EMAIL, ADMIN_PASSWORD, "192.0.2.1"));

        assertThat(resultat.isAuthenticated()).isTrue();
        assertThat(resultat.getCredentials()).isNull();
        assertThat(resultat.getAuthorities()).extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_BACKOFFICE_ADMIN");
    }

    @Test
    @DisplayName("Identifiants vides ou absents → BadCredentialsException, pas d'erreur technique")
    void identifiantsVidesOuAbsents_refuses() {
        assertThatThrownBy(() -> provider.authenticate(tentative("", "", "192.0.2.2")))
                .isInstanceOf(BadCredentialsException.class);
        assertThatThrownBy(() -> provider.authenticate(tentative(null, null, "192.0.2.3")))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    @DisplayName("Plafond global (100) atteint depuis plusieurs IP → bloqué même avec le bon mot de passe")
    void plafondGlobal_bloqueToutesLesIp() {
        // 20 IP × 5 essais : le plafond par IP n'est jamais atteint, le plafond global si
        for (int ip = 0; ip < 20; ip++) {
            for (int essai = 0; essai < 5; essai++) {
                Authentication mauvais = tentative(ADMIN_EMAIL, "mauvais", "203.0.113." + ip);
                assertThatThrownBy(() -> provider.authenticate(mauvais)).isInstanceOf(BadCredentialsException.class);
            }
        }

        assertThatThrownBy(() -> provider.authenticate(tentative(ADMIN_EMAIL, ADMIN_PASSWORD, "198.51.100.200")))
                .isInstanceOf(TropDeTentativesBackOfficeException.class);
    }

    @Test
    @DisplayName("Plafond global en recharge progressive (pas de blocage d'une heure après une rafale)")
    void plafondGlobal_rechargeProgressive() {
        assertThat(LimiteTentatives.BACKOFFICE_LOGIN_GLOBAL.isRechargeProgressive()).isTrue();
        assertThat(LimiteTentatives.BACKOFFICE_LOGIN_GLOBAL.configuration().getBandwidths()[0].isRefillIntervally())
                .isFalse();
        assertThat(LimiteTentatives.BACKOFFICE_LOGIN_PAR_IP.configuration().getBandwidths()[0].isRefillIntervally())
                .isTrue();
    }

    private static Authentication tentative(String email, String motDePasse, String ip) {
        UsernamePasswordAuthenticationToken token = UsernamePasswordAuthenticationToken.unauthenticated(email, motDePasse);
        token.setDetails(new WebAuthenticationDetails(ip, null));
        return token;
    }
}
