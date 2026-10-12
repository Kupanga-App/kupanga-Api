package com.kupanga.api.config;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * D3-D6 : garde-fous sur {@code application-prod.yml}. Une régression (Swagger réouvert, Sentry à 100 % ou avec
 * données personnelles, Flyway sans validation, détails du health exposés) fait échouer la CI.
 */
@DisplayName("Configuration de production (D3-D6)")
class ProdConfigTest {

    private static Properties prod;

    @BeforeAll
    static void charger() {
        YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(new ClassPathResource("application-prod.yml"));
        prod = yaml.getObject();
    }

    @Test
    @DisplayName("D3 : health sans détails, seuls health et info exposés")
    void actuator() {
        assertThat(prod.getProperty("management.endpoint.health.show-details")).isEqualTo("never");
        assertThat(prod.getProperty("management.endpoints.web.exposure.include")).isEqualTo("health,info");
    }

    @Test
    @DisplayName("D4 : Swagger désactivé")
    void swagger() {
        assertThat(prod.getProperty("springdoc.api-docs.enabled")).isEqualTo("false");
        assertThat(prod.getProperty("springdoc.swagger-ui.enabled")).isEqualTo("false");
    }

    @Test
    @DisplayName("D5 : Sentry sans données personnelles, échantillonnage des traces ≤ 20 %")
    void sentry() {
        assertThat(prod.getProperty("sentry.send-default-pii")).isEqualTo("false");
        assertThat(Double.parseDouble(prod.getProperty("sentry.traces-sample-rate"))).isBetween(0.0, 0.2);
    }

    @Test
    @DisplayName("D6 : Flyway valide les checksums, Hibernate ne touche jamais au schéma")
    void flyway() {
        assertThat(prod.getProperty("spring.flyway.validate-on-migrate")).isEqualTo("true");
        assertThat(prod.getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("validate");
    }

    @Test
    @DisplayName("D2/B11 : arrêt propre (requêtes et e-mails en cours terminés)")
    void arretPropre() {
        assertThat(prod.getProperty("server.shutdown")).isEqualTo("graceful");
        assertThat(prod.getProperty("spring.lifecycle.timeout-per-shutdown-phase")).isEqualTo("30s");
    }
}
