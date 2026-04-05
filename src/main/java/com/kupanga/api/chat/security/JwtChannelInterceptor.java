package com.kupanga.api.chat.security;


import com.kupanga.api.authentification.utils.JwtUtils;
import com.kupanga.api.user.entity.Role;
import com.kupanga.api.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtChannelInterceptor implements ChannelInterceptor {

    private final JwtUtils jwtUtils;
    private final UserService userService;

    /**
     * Intercepte le message CONNECT et valide le JWT transmis
     * dans le header STOMP "Authorization".
     * Le front doit envoyer :
     *   stompClient.connect({ Authorization: "Bearer <token>" }, ...)
     */
    @Override
    public Message<?> preSend(@NotNull Message<?> message, @NotNull MessageChannel channel) {

        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null) return message;

        // On n'intercepte que le CONNECT initial
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {

            String authHeader = accessor.getFirstNativeHeader("Authorization");

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                log.warn("WebSocket CONNECT sans token JWT — connexion refusée");
                throw new IllegalArgumentException("Token JWT manquant ou invalide");
            }

            String token = authHeader.substring(7);

            try {
                // Valider le token via votre JwtUtils existant
                String email = jwtUtils.extractUserEmail(token);
                Role role  = userService.getUserByEmail(email).getRole();

                if (email != null && jwtUtils.isTokenValid(token, email)) {
                    UsernamePasswordAuthenticationToken auth =
                            new UsernamePasswordAuthenticationToken(
                                    email,
                                    null,
                                    List.of(new SimpleGrantedAuthority(role.name()))
                            );
                    // Associer l'utilisateur authentifié à la session WebSocket
                    accessor.setUser(auth);
                    log.info("WebSocket CONNECT accepté pour {}", email);
                } else {
                    throw new IllegalArgumentException("Token JWT invalide ou expiré");
                }

            } catch (Exception e) {
                log.error("Erreur validation JWT WebSocket : {}", e.getMessage());
                throw new IllegalArgumentException("Token JWT invalide : " + e.getMessage());
            }
        }

        return message;
    }
}
