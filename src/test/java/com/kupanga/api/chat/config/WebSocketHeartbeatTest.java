package com.kupanga.api.chat.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.messaging.simp.broker.SimpleBrokerMessageHandler;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * W4 : le broker STOMP réellement démarré envoie et attend des battements de cœur toutes les 10 s.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@DisplayName("Tests — battements de cœur WebSocket (W4)")
class WebSocketHeartbeatTest {

    @Autowired private SimpleBrokerMessageHandler simpleBrokerMessageHandler;

    /** Le front (stompjs) demande 10 s et coupe après 2 × 10 s sans rien recevoir. */
    private static final long BATTEMENT_FRONT_MS = 10_000;

    @Test
    @DisplayName("Broker simple : heartbeat 5 s / 10 s avec un ordonnanceur (sinon négocié à 0,0)")
    void heartbeatActive() {
        assertThat(simpleBrokerMessageHandler.getHeartbeatValue()).containsExactly(5_000L, 10_000L);
        assertThat(simpleBrokerMessageHandler.getTaskScheduler()).isNotNull();
    }

    @Test
    @DisplayName("Écart maximal entre deux battements serveur (vérification + intervalle négocié) < seuil de coupure du front")
    void ecartMaximal_sousLeSeuilDuFront() {
        long[] heartbeat = simpleBrokerMessageHandler.getHeartbeatValue();
        long intervalleNegocie = Math.max(heartbeat[0], BATTEMENT_FRONT_MS);
        long periodeVerification = Math.min(heartbeat[0], heartbeat[1]);

        assertThat(intervalleNegocie + periodeVerification).isLessThan(2 * BATTEMENT_FRONT_MS);
    }
}
