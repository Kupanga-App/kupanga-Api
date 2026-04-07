package com.kupanga.api.chat.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.kupanga.api.chat.security.JwtChannelInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
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
