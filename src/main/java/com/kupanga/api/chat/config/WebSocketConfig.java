package com.kupanga.api.chat.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.kupanga.api.chat.security.JwtChannelInterceptor;
import com.kupanga.api.config.CorsProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.messaging.converter.DefaultContentTypeResolver;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.List;

@Configuration
@EnableWebSocketMessageBroker
@EnableConfigurationProperties(CorsProperties.class)
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /**
     * W4 : battements de cœur en millisecondes {serveur → client, client → serveur}.
     * Le front (stompjs) demande 10 s et coupe après 20 s sans rien recevoir. Le broker vérifie toutes les
     * {@code min} = 5 s s'il doit écrire (intervalle négocié 10 s) : l'écart réel reste de 10 à 15 s, sous les 20 s.
     * Avec 10 000 ici, l'écart pouvait atteindre ~20 s après un message poussé (déconnexions sur réseau lent).
     * Client muet : session fermée par le serveur après 3 × 10 s.
     */
    static final long[] HEARTBEAT_MS = {5_000, 10_000};

    private final JwtChannelInterceptor jwtChannelInterceptor;
    private final CorsProperties corsProperties;
    private final TaskScheduler messageBrokerTaskScheduler;

    /**
     * {@code messageBrokerTaskScheduler} : ordonnanceur déjà créé par Spring WebSocket, injecté en {@link Lazy}
     * car il est défini par la même configuration (sinon dépendance circulaire).
     */
    public WebSocketConfig(JwtChannelInterceptor jwtChannelInterceptor,
                           CorsProperties corsProperties,
                           @Lazy @Qualifier("messageBrokerTaskScheduler") TaskScheduler messageBrokerTaskScheduler) {
        this.jwtChannelInterceptor = jwtChannelInterceptor;
        this.corsProperties = corsProperties;
        this.messageBrokerTaskScheduler = messageBrokerTaskScheduler;
    }

    /**
     * Configure le broker de messages.
     * /topic  → broadcast à tous les abonnés (conversations publiques)
     * /queue  → messages ciblés à un utilisateur spécifique
     * /app    → préfixe des endpoints @MessageMapping côté serveur
     * W4 : sans ordonnanceur, le broker simple n'envoie aucun battement (négociation « 0,0 ») et les proxys / NAT
     * des réseaux mobiles coupent la connexion inactive au bout de 30 à 60 s sans que le client s'en aperçoive.
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue")
                .setHeartbeatValue(HEARTBEAT_MS)
                .setTaskScheduler(messageBrokerTaskScheduler);
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    /**
     * Endpoint de connexion WebSocket avec fallback SockJS.
     * Le front se connecte sur : ws://localhost:8089/ws
     * Seules les origines de {@code app.cors.allowed-origins} sont acceptées (W5).
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOrigins(corsProperties.allowedOrigins().toArray(String[]::new))
                .withSockJS();
    }

    /**
     * Intercepteur sur le canal entrant pour valider le JWT
     * lors du CONNECT WebSocket.
     */
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(jwtChannelInterceptor);
    }

    /**
     * CRITIQUE : configure le convertisseur JSON utilisé par le broker STOMP.
     * Ce pipeline est INDÉPENDANT du Jackson HTTP — sans ce fix, LocalDateTime
     * dans MessageDTO déclenche une exception silencieuse et le push vers B
     * n'arrive jamais.
     */


    @Override
    public boolean configureMessageConverters(List<MessageConverter> messageConverters) {
        // 1. Résolveur de content-type : on force application/json
        DefaultContentTypeResolver resolver = new DefaultContentTypeResolver();
        resolver.setDefaultMimeType(MimeTypeUtils.APPLICATION_JSON);

        // 2. Configuration de l'ObjectMapper (Dates ISO-8601)
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // 3. Utilisation du BON convertisseur pour Messaging/Websocket
        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        converter.setObjectMapper(mapper);
        converter.setContentTypeResolver(resolver);

        messageConverters.add(converter);

        // Retourner false pour désactiver les convertisseurs par défaut
        return false;
    }
}
