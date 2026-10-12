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
     * B11 : exécuteur réservé aux e-mails, pour qu'ils ne restent pas derrière des tâches lentes (POI, ~23 s).
     * File pleine : l'envoi se fait dans le thread appelant plutôt que d'être perdu ou de faire échouer
     * une requête déjà validée en base (rejet levé dans afterCommit).
     */
    @Bean(name = "emailExecutor")
    public Executor emailExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("email-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        // Arrêt (déploiement) : les e-mails en file partent avant la fermeture, sinon ils seraient perdus
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);

        executor.initialize();
        return executor;
    }
}
