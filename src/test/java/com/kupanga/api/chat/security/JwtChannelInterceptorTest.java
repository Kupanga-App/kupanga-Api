package com.kupanga.api.chat.security;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.kupanga.api.authentification.utils.JwtUtils;
import com.kupanga.api.user.entity.Role;
import com.kupanga.api.user.entity.User;
import com.kupanga.api.user.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.security.Principal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * W3 : abonnements limités à /user/queue/**, envois limités à /app/**, commandes du serveur refusées,
 * utilisateur authentifié exigé.
 */
@DisplayName("Tests unitaires — JwtChannelInterceptor (W3)")
class JwtChannelInterceptorTest {

    private static final Principal ALICE =
            new UsernamePasswordAuthenticationToken("alice@test.com", null, List.of());

    private final JwtUtils jwtUtils = mock(JwtUtils.class);
    private final UserService userService = mock(UserService.class);
    private final JwtChannelInterceptor interceptor = new JwtChannelInterceptor(jwtUtils, userService);
    private final MessageChannel canal = mock(MessageChannel.class);

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"/user/queue/messages", "/user/queue/notifications",
            "/user/queue/app-notifications", "/user/queue/errors"})
    @DisplayName("SUBSCRIBE sur une file privée de l'utilisateur → accepté")
    void subscribe_filePrivee_accepte(String destination) {
        Message<byte[]> trame = trame(StompCommand.SUBSCRIBE, destination, ALICE);

        assertThat(interceptor.preSend(trame, canal)).isSameAs(trame);
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"/topic/annonces", "/queue/messages-user0a1b2c", "/queue/messages",
            "/user/bob@test.com/queue/messages", "/user/queue/**", "/user/queue/*", "/user/queue/",
            "/app/chat.send", "/user/topic/x", "/USER/queue/messages"})
    @DisplayName("SUBSCRIBE ailleurs (topic, file d'une autre session, motif…) → ignoré")
    void subscribe_horsFilePrivee_ignore(String destination) {
        assertThat(interceptor.preSend(trame(StompCommand.SUBSCRIBE, destination, ALICE), canal)).isNull();
    }

    @Test
    @DisplayName("SEND vers un @MessageMapping → accepté")
    void send_app_accepte() {
        Message<byte[]> trame = trame(StompCommand.SEND, "/app/chat.send", ALICE);

        assertThat(interceptor.preSend(trame, canal)).isSameAs(trame);
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"/user/bob@test.com/queue/messages", "/user/queue/messages",
            "/queue/messages-user0a1b2c", "/topic/annonces"})
    @DisplayName("SEND direct au broker (faux message à un autre utilisateur) → ignoré")
    void send_horsApp_ignore(String destination) {
        assertThat(interceptor.preSend(trame(StompCommand.SEND, destination, ALICE), canal)).isNull();
        verifyNoInteractions(jwtUtils, userService);
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = StompCommand.class, names = {"MESSAGE", "RECEIPT", "ERROR", "CONNECTED"})
    @DisplayName("Commande réservée au serveur (ex. MESSAGE, relayée au broker comme un SEND) → ignorée")
    void commandeServeur_ignoree(StompCommand commande) {
        Message<byte[]> trame = trame(commande, "/user/bob@test.com/queue/messages", ALICE);

        assertThat(interceptor.preSend(trame, canal)).isNull();
    }

    @Test
    @DisplayName("SUBSCRIBE et SEND sans utilisateur authentifié → ignorés")
    void sansUtilisateur_ignore() {
        assertThat(interceptor.preSend(trame(StompCommand.SUBSCRIBE, "/user/queue/messages", null), canal)).isNull();
        assertThat(interceptor.preSend(trame(StompCommand.SEND, "/app/chat.send", null), canal)).isNull();
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = StompCommand.class, names = {"CONNECT", "STOMP"})
    @DisplayName("CONNECT et STOMP sans JWT → refusés (STOMP ne contourne plus l'authentification)")
    void connexionSansJwt_refusee(StompCommand commande) {
        assertThatThrownBy(() -> interceptor.preSend(trame(commande, null, null), canal))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("W10 : CONNECT accepté ou refusé → ni e-mail ni jeton dans les logs, rien au-dessus de DEBUG")
    void connexion_logsSansEmailNiJeton() {
        when(jwtUtils.extractUserEmail("jeton-valide")).thenReturn("alice@test.com");
        when(jwtUtils.isTokenValid("jeton-valide", "alice@test.com")).thenReturn(true);
        when(userService.getUserByEmail("alice@test.com"))
                .thenReturn(User.builder().mail("alice@test.com").role(Role.ROLE_LOCATAIRE).build());
        when(jwtUtils.extractUserEmail("jeton-invalide"))
                .thenThrow(new IllegalStateException("JWT invalide jeton-invalide pour alice@test.com"));

        Logger logger = (Logger) LoggerFactory.getLogger(JwtChannelInterceptor.class);
        Level niveauInitial = logger.getLevel();
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        logger.setLevel(Level.TRACE);
        try {
            assertThat(interceptor.preSend(connexion("Bearer jeton-valide"), canal)).isNotNull();
            assertThatThrownBy(() -> interceptor.preSend(connexion("Bearer jeton-invalide"), canal))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> interceptor.preSend(connexion(null), canal))
                    .isInstanceOf(IllegalArgumentException.class);
        } finally {
            logger.detachAppender(appender);
            logger.setLevel(niveauInitial);
        }

        assertThat(appender.list).isNotEmpty().allSatisfy(evenement -> {
            assertThat(evenement.getLevel()).isEqualTo(Level.DEBUG);
            assertThat(evenement.getFormattedMessage()).doesNotContain("alice@test.com", "jeton-");
            assertThat(evenement.getThrowableProxy()).isNull();
        });
    }

    private static Message<byte[]> connexion(String authorization) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        if (authorization != null) accessor.addNativeHeader("Authorization", authorization);
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private static Message<byte[]> trame(StompCommand commande, String destination, Principal utilisateur) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(commande);
        if (destination != null) accessor.setDestination(destination);
        if (utilisateur != null) accessor.setUser(utilisateur);
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }
}
