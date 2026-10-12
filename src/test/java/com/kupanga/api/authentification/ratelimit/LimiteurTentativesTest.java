package com.kupanga.api.authentification.ratelimit;

import com.kupanga.api.exception.business.TropDeTentativesException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;

import static com.kupanga.api.authentification.ratelimit.LimiteTentatives.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * A3 : limite de tentatives (stockage mémoire, même logique qu'avec Redis).
 */
@DisplayName("Tests unitaires — LimiteurTentatives")
class LimiteurTentativesTest {

    private final LimiteurTentatives limiteur = new LimiteurTentatives(new RateLimitConfig().stockageSeauxMemoire());

    @Test
    @DisplayName("Login : 5 essais par (e-mail, IP), le 6e est refusé (429, Retry-After ≤ 15 min)")
    void login_sixiemeEssaiParEmailEtIp_refuse() {
        MockHttpServletRequest attaquant = requeteDepuis("203.0.113.66");
        for (int i = 0; i < 5; i++) {
            limiteur.verifierEmailEtIp(LOGIN_PAR_EMAIL_ET_IP, "alice@test.com", attaquant);
        }

        assertThatThrownBy(() -> limiteur.verifierEmailEtIp(LOGIN_PAR_EMAIL_ET_IP, "alice@test.com", attaquant))
                .isInstanceOfSatisfying(TropDeTentativesException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
                    assertThat(e.getRetryAfterSecondes()).isBetween(1L, 15 * 60L + 1);
                    assertThat(e.getMessage()).doesNotContain("alice").doesNotContain("203.0.113.66");
                });
    }

    @Test
    @DisplayName("Login : un tiers qui force le compte ne bloque pas la victime depuis sa propre IP")
    void login_tiersNeBloquePasLaVictime() {
        MockHttpServletRequest attaquant = requeteDepuis("203.0.113.66");
        MockHttpServletRequest victime = requeteDepuis("198.51.100.7");
        for (int i = 0; i < 5; i++) {
            limiteur.verifierEmailEtIp(LOGIN_PAR_EMAIL_ET_IP, "alice@test.com", attaquant);
        }

        assertThatCode(() -> limiteur.verifierEmailEtIp(LOGIN_PAR_EMAIL_ET_IP, "alice@test.com", victime))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Login : plafond global de 30 essais par e-mail (attaque répartie sur plusieurs IP)")
    void login_plafondGlobalParEmail() {
        for (int i = 0; i < 30; i++) {
            limiteur.verifierEmail(LOGIN_PAR_EMAIL, "alice@test.com");
        }

        assertThatThrownBy(() -> limiteur.verifierEmail(LOGIN_PAR_EMAIL, "alice@test.com"))
                .isInstanceOf(TropDeTentativesException.class);
    }

    @Test
    @DisplayName("Forgot-password : 20 demandes par heure et par IP, quels que soient les e-mails visés")
    void forgot_parIp_tousEmailsConfondus() {
        MockHttpServletRequest ip = requeteDepuis("203.0.113.9");
        for (int i = 0; i < 20; i++) {
            limiteur.verifierIp(FORGOT_PASSWORD_PAR_IP, ip);
            limiteur.verifierEmail(FORGOT_PASSWORD_PAR_EMAIL, "compte" + i + "@test.com");
        }

        assertThatThrownBy(() -> limiteur.verifierIp(FORGOT_PASSWORD_PAR_IP, ip))
                .isInstanceOf(TropDeTentativesException.class);
    }

    @Test
    @DisplayName("La casse et les espaces de l'e-mail ne permettent pas de contourner la limite")
    void email_normalise() {
        limiteur.verifierEmail(FORGOT_PASSWORD_PAR_EMAIL, "Bob@Test.com");
        limiteur.verifierEmail(FORGOT_PASSWORD_PAR_EMAIL, " bob@test.com ");
        limiteur.verifierEmail(FORGOT_PASSWORD_PAR_EMAIL, "BOB@TEST.COM");

        assertThatThrownBy(() -> limiteur.verifierEmail(FORGOT_PASSWORD_PAR_EMAIL, "bob@test.com"))
                .isInstanceOf(TropDeTentativesException.class);
    }

    @Test
    @DisplayName("Register : 5 par heure et par IP ; une autre IP et une autre limite restent libres")
    void register_parIp_independant() {
        MockHttpServletRequest ip1 = requeteDepuis("203.0.113.1");
        MockHttpServletRequest ip2 = requeteDepuis("203.0.113.2");
        for (int i = 0; i < 5; i++) {
            limiteur.verifierIp(REGISTER_PAR_IP, ip1);
        }

        assertThatThrownBy(() -> limiteur.verifierIp(REGISTER_PAR_IP, ip1))
                .isInstanceOf(TropDeTentativesException.class);
        assertThatCode(() -> limiteur.verifierIp(REGISTER_PAR_IP, ip2)).doesNotThrowAnyException();
        assertThatCode(() -> limiteur.verifierIp(GOOGLE_PAR_IP, ip1)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Stockage indisponible (Redis en panne) : la requête passe, pas de 500")
    void stockageIndisponible_laissePasser() {
        LimiteurTentatives enPanne = new LimiteurTentatives((cle, configuration) -> {
            throw new IllegalStateException("Redis injoignable");
        });

        assertThatCode(() -> enPanne.verifierEmail(LOGIN_PAR_EMAIL, "alice@test.com")).doesNotThrowAnyException();
    }

    private MockHttpServletRequest requeteDepuis(String ip) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr(ip);
        return request;
    }
}
