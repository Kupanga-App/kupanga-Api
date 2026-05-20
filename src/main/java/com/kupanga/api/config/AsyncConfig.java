package com.kupanga.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

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
}
