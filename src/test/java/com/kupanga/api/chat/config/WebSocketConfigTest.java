package com.kupanga.api.chat.config;

import com.kupanga.api.chat.security.JwtChannelInterceptor;
import com.kupanga.api.config.CorsProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.StompWebSocketEndpointRegistration;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * W5 : le endpoint STOMP /ws n'accepte que les origines configurées (jamais « * »).
 */
@DisplayName("Tests unitaires — WebSocketConfig")
class WebSocketConfigTest {

    @Test
    @DisplayName("/ws enregistré avec les origines de app.cors.allowed-origins, sans joker")
    void registerStompEndpoints_originesRestreintes() {
        StompEndpointRegistry registry = mock(StompEndpointRegistry.class);
        StompWebSocketEndpointRegistration registration = mock(StompWebSocketEndpointRegistration.class, RETURNS_SELF);
        when(registry.addEndpoint("/ws")).thenReturn(registration);

        new WebSocketConfig(mock(JwtChannelInterceptor.class),
                new CorsProperties(List.of("http://localhost:4200", "https://kupanga.lespacelibellule.com")))
                .registerStompEndpoints(registry);

        verify(registration).setAllowedOrigins("http://localhost:4200", "https://kupanga.lespacelibellule.com");
        verify(registration, never()).setAllowedOriginPatterns(any(String[].class));
    }
}
