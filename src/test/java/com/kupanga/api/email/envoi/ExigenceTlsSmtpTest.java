package com.kupanga.api.email.envoi;

import com.kupanga.api.config.PlanificationConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Revue M2 : SMTP chiffré exigé en prod, ordonnanceur dédié aux tâches planifiées")
class ExigenceTlsSmtpTest {

    @Test
    @DisplayName("Ni STARTTLS exigé ni SSL → démarrage refusé ; l'un ou l'autre → accepté")
    void tlsExige() {
        assertThatThrownBy(() -> ExigenceTlsSmtp.verifier(false, false))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("SMTP_STARTTLS");
        assertThatCode(() -> ExigenceTlsSmtp.verifier(true, false)).doesNotThrowAnyException();
        assertThatCode(() -> ExigenceTlsSmtp.verifier(false, true)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Tâches planifiées sur leur propre ordonnanceur (pas celui du broker STOMP)")
    void ordonnanceurDedie() {
        assertThat(new PlanificationConfig().taskScheduler().getThreadNamePrefix()).isEqualTo("planif-");
    }
}
