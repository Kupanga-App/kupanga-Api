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

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtChannelInterceptor implements ChannelInterceptor {

    /** Files privées de l'utilisateur connecté (résolues par Spring vers sa seule session). */
    static final String PREFIXE_ABONNEMENT = "/user/queue/";
    /** Endpoints @MessageMapping (ex. /app/chat.send). */
    static final String PREFIXE_ENVOI = "/app/";
    /** Commandes qu'un client STOMP a le droit d'envoyer ; les autres (MESSAGE, RECEIPT, ERROR…) sont du serveur. */
    private static final Set<StompCommand> COMMANDES_CLIENT = EnumSet.of(
            StompCommand.CONNECT, StompCommand.STOMP, StompCommand.DISCONNECT,
            StompCommand.SEND, StompCommand.SUBSCRIBE, StompCommand.UNSUBSCRIBE,
            StompCommand.ACK, StompCommand.NACK, StompCommand.BEGIN, StompCommand.COMMIT, StompCommand.ABORT);

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

        // W3 : abonnements limités aux files privées de l'utilisateur (/user/queue/**) et envois limités
        // aux @MessageMapping (/app/**) : sinon un client peut écouter /topic/** ou la file d'une autre session,
        // ou envoyer directement à /user/{email}/queue/messages en contournant les règles de MessageServiceImpl.
        // Trame interdite ignorée (null) plutôt qu'une erreur qui fermerait toute la connexion.
        StompCommand commande = accessor.getCommand();
        if (commande == null) return message; // battement de cœur
        if (!COMMANDES_CLIENT.contains(commande)) {
            // MESSAGE, RECEIPT… sont des trames du serveur : Spring les relaierait au broker comme un SEND
            log.debug("WebSocket : commande {} refusée (réservée au serveur)", commande);
            return null;
        }
        boolean abonnement = StompCommand.SUBSCRIBE.equals(commande);
        if (abonnement || StompCommand.SEND.equals(commande)) {
            String prefixe = abonnement ? PREFIXE_ABONNEMENT : PREFIXE_ENVOI;
            if (accessor.getUser() == null || !destinationAutorisee(accessor.getDestination(), prefixe)) {
                log.debug("WebSocket {} refusé : utilisateur non authentifié ou destination non autorisée", commande);
                return null;
            }
        }

        // CONNECT initial (STOMP est son équivalent : Spring le traite de la même façon)
        if (StompCommand.CONNECT.equals(commande) || StompCommand.STOMP.equals(commande)) {

            String authHeader = accessor.getFirstNativeHeader("Authorization");

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                log.debug("WebSocket CONNECT sans token JWT — connexion refusée");
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
                    log.debug("WebSocket CONNECT accepté");
                } else {
                    throw new IllegalArgumentException("Token JWT invalide ou expiré");
                }

            } catch (Exception e) {
                // W10 : jeton invalide = cas courant (expiration), sans détail : le message peut citer le jeton
                log.debug("WebSocket CONNECT refusé : {}", e.getClass().getSimpleName());
                throw new IllegalArgumentException("Token JWT invalide ou expiré");
            }
        }

        return message;
    }

    /** Préfixe, sans motif (* ? {) : le broker simple interprète les destinations d'abonnement comme des motifs. */
    private static boolean destinationAutorisee(String destination, String prefixe) {
        return destination != null
                && destination.startsWith(prefixe)
                && destination.length() > prefixe.length()
                && destination.chars().noneMatch(c -> c == '*' || c == '?' || c == '{' || c == '}');
    }
}
