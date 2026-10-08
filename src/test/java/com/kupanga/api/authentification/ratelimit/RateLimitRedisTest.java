package com.kupanga.api.authentification.ratelimit;

import com.kupanga.api.exception.business.TropDeTentativesException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.UUID;

import static com.kupanga.api.authentification.ratelimit.LimiteTentatives.LOGIN_PAR_EMAIL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * A3 : stockage Redis réel (client Lettuce de Spring, CAS Bucket4j, préfixe et expiration des clés).
 * Ignoré si aucun Redis n'écoute sur localhost:6379 (CI) ; en local : redis-dev de docker-compose-dev.yml.
 */
@DisplayName("Tests d'intégration — limite de tentatives sur Redis")
class RateLimitRedisTest {

    private LettuceConnectionFactory connectionFactory;

    @BeforeEach
    void setUp() {
        connectionFactory = new LettuceConnectionFactory(new RedisStandaloneConfiguration("localhost", 6379));
        connectionFactory.afterPropertiesSet();
        connectionFactory.start();
        boolean redisDisponible;
        try (var connexion = connectionFactory.getConnection()) {
            redisDisponible = "PONG".equals(connexion.ping());
        } catch (RuntimeException e) {
            redisDisponible = false;
        }
        assumeTrue(redisDisponible, "Redis absent sur localhost:6379 : test ignoré");
    }

    @AfterEach
    void tearDown() {
        connectionFactory.destroy();
    }

    @Test
    @DisplayName("Redis : le 31e essai sur un e-mail est refusé et la clé expire d'elle-même")
    void redis_limiteEtExpiration() {
        LimiteurTentatives limiteur = new LimiteurTentatives(new RateLimitConfig().stockageSeauxRedis(connectionFactory));
        String email = "redis-" + UUID.randomUUID() + "@test.com";

        for (int i = 0; i < LOGIN_PAR_EMAIL.getCapacite(); i++) {
            limiteur.verifierEmail(LOGIN_PAR_EMAIL, email);
        }

        assertThatThrownBy(() -> limiteur.verifierEmail(LOGIN_PAR_EMAIL, email))
                .isInstanceOf(TropDeTentativesException.class);

        String cle = "kupanga:rate-limit:login-email:" + email;
        StringRedisTemplate redis = new StringRedisTemplate(connectionFactory);
        assertThat(redis.hasKey(cle)).isTrue();
        assertThat(redis.getExpire(cle)).isPositive();
        redis.delete(cle);
    }
}
