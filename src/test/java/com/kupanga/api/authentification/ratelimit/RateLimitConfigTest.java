package com.kupanga.api.authentification.ratelimit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

import java.time.Duration;

import static com.kupanga.api.authentification.ratelimit.LimiteTentatives.LOGIN_PAR_EMAIL;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Déploiement Render : Redis injoignable au démarrage ne doit pas empêcher l'application de démarrer (fail-open A3).
 */
@DisplayName("Tests unitaires — stockage Redis de la limite de tentatives")
class RateLimitConfigTest {

    private LettuceConnectionFactory connectionFactory;

    @AfterEach
    void tearDown() {
        if (connectionFactory != null) connectionFactory.destroy();
    }

    private LettuceConnectionFactory redisInjoignable() {
        // Port 1 : rien n'y écoute, la connexion est refusée tout de suite
        connectionFactory = new LettuceConnectionFactory(
                new RedisStandaloneConfiguration("localhost", 1),
                LettuceClientConfiguration.builder().commandTimeout(Duration.ofMillis(500)).build());
        connectionFactory.afterPropertiesSet();
        connectionFactory.start();
        return connectionFactory;
    }

    @Test
    @DisplayName("Redis injoignable : le stockage se crée sans se connecter")
    void creation_sansConnexion() {
        LettuceConnectionFactory factory = redisInjoignable();

        assertThatCode(() -> new RateLimitConfig().stockageSeauxRedis(factory)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Redis injoignable : le seau lève une erreur, retentée à chaque appel")
    void seau_erreurRetentee() {
        StockageSeaux stockage = new RateLimitConfig().stockageSeauxRedis(redisInjoignable());

        assertThatThrownBy(() -> stockage.seau("cle", LOGIN_PAR_EMAIL.configuration())).isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> stockage.seau("cle", LOGIN_PAR_EMAIL.configuration())).isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("Redis injoignable : le limiteur laisse passer (fail-open)")
    void limiteur_failOpen() {
        LimiteurTentatives limiteur = new LimiteurTentatives(new RateLimitConfig().stockageSeauxRedis(redisInjoignable()));

        assertThatCode(() -> limiteur.verifierEmail(LOGIN_PAR_EMAIL, "a@test.com")).doesNotThrowAnyException();
    }
}
