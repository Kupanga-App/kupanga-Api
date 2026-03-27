package com.kupanga.api.chat.config;

import com.kupanga.api.chat.security.JwtChannelInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtChannelInterceptor jwtChannelInterceptor;

    /**
     * Configure le broker de messages.
     * /topic  → broadcast à tous les abonnés (conversations publiques)
     * /queue  → messages ciblés à un utilisateur spécifique
     * /app    → préfixe des endpoints @MessageMapping côté serveur
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    /**
     * Endpoint de connexion WebSocket avec fallback SockJS.
     * Le front se connecte sur : ws://localhost:8089/ws
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")  // à restreindre en prod
                .withSockJS(); //à remettre pour la connexion front et fallback SockJS pour les navigateurs sans WS natif
    }

    /**
     * Intercepteur sur le canal entrant pour valider le JWT
     * lors du CONNECT WebSocket.
     */
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(jwtChannelInterceptor);
    }
}
