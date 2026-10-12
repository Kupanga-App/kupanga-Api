package com.kupanga.api.chat.controller;

import com.kupanga.api.exception.business.UserNotFoundException;
import com.kupanga.api.exception.business.InvalidRoleException;
import com.kupanga.api.chat.dto.MessagePayload;
import com.kupanga.api.chat.service.MessageService;
import com.kupanga.api.exception.business.KupangaBusinessException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.security.Principal;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * W2 : validation du payload WebSocket et retour d'erreur à l'expéditeur.
 */
class MessagePayloadValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
    private final MessageService messageService = mock(MessageService.class);
    private final MessageController controller = new MessageController(messageService);

    @Test
    @DisplayName("Message de plus de 4000 caractères refusé")
    void contenuTropLong_refuse() {
        Set<ConstraintViolation<MessagePayload>> violations =
                validator.validate(new MessagePayload("a".repeat(4001), null, 1L));

        assertThat(violations).extracting(v -> v.getPropertyPath().toString()).contains("contenu");
    }

    @Test
    @DisplayName("Contenu vide ou bienId absent refusés")
    void contenuVideOuBienAbsent_refuses() {
        assertThat(validator.validate(new MessagePayload("  ", null, 1L))).isNotEmpty();
        assertThat(validator.validate(new MessagePayload("Bonjour", null, null))).isNotEmpty();
    }

    @Test
    @DisplayName("Message valide (sans e-mail destinataire) accepté")
    void messageValide_accepte() {
        assertThat(validator.validate(new MessagePayload("Bonjour", null, 1L))).isEmpty();
        assertThat(validator.validate(new MessagePayload("a".repeat(4000), "bob@test.com", 1L))).isEmpty();
    }

    @Test
    @DisplayName("Erreur métier renvoyée telle quelle, erreur technique masquée (pas de détail interne)")
    void handleErreurMessage_neDivulguePasLesErreursTechniques() {
        Map<String, String> metier = controller.handleErreurMessage(
                new KupangaBusinessException("Impossible d'envoyer un message à soi-même", HttpStatus.BAD_REQUEST), null);
        Map<String, String> technique = controller.handleErreurMessage(
                new IllegalStateException("org.hibernate... détail interne"), null);

        assertThat(metier.get("message")).isEqualTo("Impossible d'envoyer un message à soi-même");
        assertThat(technique.get("message")).isEqualTo("Le message n'a pas pu être envoyé.")
                .doesNotContain("hibernate");
    }

    @Test
    @DisplayName("Toute BusinessException renvoyée telle quelle, sauf UserNotFoundException (générique, pas d'énumération)")
    void handleErreurMessage_businessException() {
        Map<String, String> role = controller.handleErreurMessage(new InvalidRoleException("Rôle métier invalide"), null);
        Map<String, String> inconnu = controller.handleErreurMessage(new UserNotFoundException("bob@test.com"), null);

        assertThat(role.get("message")).isEqualTo("Rôle métier invalide");
        assertThat(inconnu.get("message")).isEqualTo("Le message n'a pas pu être envoyé.")
                .doesNotContain("bob@test.com");
    }

    @Test
    @DisplayName("W13 : erreur renvoyée avec l'idClient du front pour passer le message en « échec »")
    void handleErreurMessage_renvoieIdClient() {
        Map<String, String> erreur = controller.handleErreurMessage(
                new KupangaBusinessException("Ce bien n'est plus disponible", HttpStatus.FORBIDDEN),
                "0b1c2d3e-4f50-6172-8394-a5b6c7d8e9f0");

        assertThat(erreur).containsEntry("idClient", "0b1c2d3e-4f50-6172-8394-a5b6c7d8e9f0")
                .containsEntry("message", "Ce bien n'est plus disponible");
        assertThat(controller.handleErreurMessage(new IllegalStateException("x"), null)).doesNotContainKey("idClient");
    }

    @Test
    @DisplayName("W13 : idClient renvoyé seulement s'il est court et sans caractère spécial (pas de réflexion de contenu)")
    void idClient_invalide_ignore() {
        assertThat(MessageController.idClientValide("abc-123")).isEqualTo("abc-123");
        assertThat(MessageController.idClientValide("<img src=x onerror=alert(1)>")).isNull();
        assertThat(MessageController.idClientValide("a".repeat(65))).isNull();
        assertThat(MessageController.idClientValide("")).isNull();
        assertThat(MessageController.idClientValide(null)).isNull();
        assertThat(controller.handleErreurMessage(new IllegalStateException("x"), "a b")).doesNotContainKey("idClient");
    }

    @Test
    @DisplayName("W13 : l'en-tête id-client est transmis au service (null s'il est invalide)")
    void sendMessage_transmetIdClient() {
        MessagePayload payload = new MessagePayload("Bonjour", null, 1L);
        Principal alice = () -> "alice@test.com";

        controller.sendMessage(payload, alice, "abc-123");
        controller.sendMessage(payload, alice, "pas valide !");

        verify(messageService).envoyerMessage(payload, "alice@test.com", "abc-123");
        verify(messageService).envoyerMessage(payload, "alice@test.com", null);
    }
}
