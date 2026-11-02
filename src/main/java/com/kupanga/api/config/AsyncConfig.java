package com.kupanga.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Configuration Spring pour activer l'exécution asynchrone.
 * Cette classe permet à Spring de gérer les méthodes annotées avec {@link org.springframework.scheduling.annotation.Async}.
 * Lorsqu'une méthode est annotée avec {@code @Async}, elle sera exécutée dans un thread séparé,
 * permettant de ne pas bloquer le thread principal et d'améliorer la réactivité de l'application.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("async-");

        executor.initialize();
        return executor;
    }

    /**
     * B11 / M2 : exécuteur réservé au réveil de l'envoyeur d'e-mails ({@code EnvoyeurEmails}), pour qu'il ne reste
     * pas derrière des tâches lentes (POI, ~23 s). Les e-mails eux-mêmes attendent en base (file persistante) :
     * un réveil refusé (file pleine) ou perdu à l'arrêt ne perd aucun e-mail, la tâche planifiée les reprend.
     * Un seul thread : l'envoyeur traite toute la file à chaque passage.
     */
    @Bean(name = "emailExecutor")
    public Executor emailExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(10);
        executor.setThreadNamePrefix("email-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardPolicy());
        // Arrêt (déploiement) : le passage en cours se termine (un envoi coupé repartirait à l'expiration du verrou)
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);

        executor.initialize();
        return executor;
    }
}
