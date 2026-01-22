package com.kupanga.api.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Configuration Spring pour activer l'exécution asynchrone.
 *
 * <p>
 * Cette classe permet à Spring de gérer les méthodes annotées avec {@link org.springframework.scheduling.annotation.Async}.
 * Lorsqu'une méthode est annotée avec {@code @Async}, elle sera exécutée dans un thread séparé,
 * permettant de ne pas bloquer le thread principal et d'améliorer la réactivité de l'application.
 * </p>
 *
 * <p>
 * Pour utiliser l'asynchrone :
 * <ul>
 *     <li>Annoter une méthode avec {@code @Async}.</li>
 *     <li>Configurer éventuellement un {@link java.util.concurrent.Executor} personnalisé pour gérer les threads.</li>
 * </ul>
 * </p>
 *
 * <p>
 * Exemple d'utilisation :
 * <pre>
 * &#64;Service
 * public class EmailService {
 *
 *     &#64;Async
 *     public void envoyerMail(String destinataire) {
 *         // Code exécuté de manière asynchrone
 *     }
 * }
 * </pre>
 * </p>
 */
@EnableAsync
@Configuration
public class AsyncConfig {
}
