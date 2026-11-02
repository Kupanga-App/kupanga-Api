package com.kupanga.api.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * M2 : tâches planifiées (reprise et purge de la file d'e-mails). Désactivées en test
 * ({@code email.envoi.actif=false}) : aucun envoi ni purge en arrière-plan pendant les tests sur base partagée.
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "email.envoi.actif", havingValue = "true", matchIfMissing = true)
public class PlanificationConfig {

    /**
     * Revue M2 : ordonnanceur propre aux tâches planifiées. Sans lui, {@code @Scheduled} prenait le seul
     * ordonnanceur existant, celui du broker STOMP (battements W4) : un envoi SMTP lent ou en panne l'aurait bloqué
     * et coupé les sessions WebSocket. Le nom {@code taskScheduler} est celui que Spring choisit quand il y en a
     * plusieurs.
     */
    @Bean(name = "taskScheduler")
    public ThreadPoolTaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler ordonnanceur = new ThreadPoolTaskScheduler();
        ordonnanceur.setPoolSize(2);
        ordonnanceur.setThreadNamePrefix("planif-");
        ordonnanceur.setWaitForTasksToCompleteOnShutdown(true);
        ordonnanceur.setAwaitTerminationSeconds(30);
        return ordonnanceur;
    }
}
