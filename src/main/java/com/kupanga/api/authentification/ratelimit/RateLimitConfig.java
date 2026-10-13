package com.kupanga.api.authentification.ratelimit;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.lettuce.Bucket4jLettuce;
import io.lettuce.core.RedisClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Stockage des compteurs de la limite de tentatives (A3).
 * {@code app.rate-limit.stockage} : {@code redis} (défaut, compteurs partagés entre instances
 * et conservés au redémarrage) ou {@code memoire} (tests, sans Redis).
 */
@Configuration
public class RateLimitConfig {

    private static final String PREFIXE_REDIS = "kupanga:rate-limit:";

    @Bean
    @ConditionalOnProperty(name = "app.rate-limit.stockage", havingValue = "redis", matchIfMissing = true)
    public StockageSeaux stockageSeauxRedis(LettuceConnectionFactory connectionFactory) {
        // Connexion ouverte au premier usage, pas au démarrage : Redis indisponible (ou lent, ex. poignée de main TLS
        // sur une petite instance) ne doit pas empêcher l'application de démarrer. L'exception remonte alors à
        // LimiteurTentatives (fail-open) et la connexion est retentée à l'appel suivant.
        AtomicReference<ProxyManager<String>> proxyManager = new AtomicReference<>();
        return (cle, configuration) -> {
            ProxyManager<String> manager = proxyManager.get();
            if (manager == null) {
                synchronized (proxyManager) {
                    manager = proxyManager.get();
                    if (manager == null) {
                        manager = creerProxyManager(connectionFactory);
                        proxyManager.set(manager);
                    }
                }
            }
            return manager.builder().build(cle, () -> configuration);
        };
    }

    private static ProxyManager<String> creerProxyManager(LettuceConnectionFactory connectionFactory) {
        // Réutilise le client Lettuce de Spring (même Redis que le cache, TLS compris en prod).
        // Redis autonome uniquement : en Cluster, utiliser Bucket4jLettuce.casBasedBuilder(RedisClusterClient).
        RedisClient client = (RedisClient) connectionFactory.getRequiredNativeClient();
        return Bucket4jLettuce.casBasedBuilder(client)
                // La clé disparaît de Redis une fois le compteur rechargé : pas d'accumulation
                .expirationAfterWrite(ExpirationAfterWriteStrategy.basedOnTimeForRefillingBucketUpToMax(Duration.ofMinutes(1)))
                .requestTimeout(Duration.ofSeconds(2))
                .build()
                .withMapper(cle -> (PREFIXE_REDIS + cle).getBytes(StandardCharsets.UTF_8));
    }

    /** Réservé aux tests : aucune éviction, la map grossirait sans fin en production. */
    @Bean
    @ConditionalOnProperty(name = "app.rate-limit.stockage", havingValue = "memoire")
    public StockageSeaux stockageSeauxMemoire() {
        Map<String, Bucket> seaux = new ConcurrentHashMap<>();
        return (cle, configuration) -> seaux.computeIfAbsent(cle, k -> {
            var builder = Bucket.builder();
            for (Bandwidth limite : configuration.getBandwidths()) {
                builder.addLimit(limite);
            }
            return builder.build();
        });
    }
}
