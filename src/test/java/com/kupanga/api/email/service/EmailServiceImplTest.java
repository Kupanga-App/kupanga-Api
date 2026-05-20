package com.kupanga.api.email.service;

import com.kupanga.api.email.client.BrevoEmailClient;
import com.kupanga.api.email.dto.BrevoEmail;
import com.kupanga.api.email.service.impl.EmailServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@DisplayName("Tests unitaires pour EmailServiceImpl")
class EmailServiceImplTest {

    private final BrevoEmailClient brevoClient = mock(BrevoEmailClient.class);

    private final EmailServiceImpl emailService = new EmailServiceImpl(
            brevoClient,
            "noreplydevback@gmail.com",
            "Kupanga",
            "http://localhost:4200/auth/reset-password?token=",
            "http://localhost:4200/auth/login",
            "http://localhost:4200/"
    );

    @Test
    @DisplayName("sendWelcomeMessage — appelle BrevoEmailClient.send() une fois")
    void sendWelcomeMessage_shouldCallBrevoClientOnce() {
        emailService.sendWelcomeMessage("test@example.com", "Alice");

        verify(brevoClient, times(1)).send(any(BrevoEmail.class));
    }

    @Test
    @DisplayName("sendWelcomeMessage — l'email est adressé au bon destinataire")
    void sendWelcomeMessage_shouldSendToCorrectRecipient() {
        String destinataire = "alice@example.com";

        emailService.sendWelcomeMessage(destinataire, "Alice");

        ArgumentCaptor<BrevoEmail> captor = ArgumentCaptor.forClass(BrevoEmail.class);
        verify(brevoClient).send(captor.capture());

        BrevoEmail sent = captor.getValue();
        assertThat(sent.to()).hasSize(1);
        assertThat(sent.to().get(0).email()).isEqualTo(destinataire);
        assertThat(sent.subject()).contains("Alice");
        assertThat(sent.attachment()).isNull();
    }

    @Test
    @DisplayName("sendWelcomeMessage — propage la RuntimeException du client Brevo")
    void sendWelcomeMessage_shouldPropagateException() {
        doThrow(new RuntimeException("Brevo KO")).when(brevoClient).send(any());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> emailService.sendWelcomeMessage("test@example.com", "Alice"));

        assertThat(ex.getMessage()).contains("Brevo KO");
    }

    @Test
    @DisplayName("sendPasswordResetMail — appelle BrevoEmailClient.send() une fois")
    void sendPasswordResetMail_shouldCallBrevoClientOnce() {
        emailService.sendPasswordResetMail("user@kupanga.com", "abc123");

        verify(brevoClient, times(1)).send(any(BrevoEmail.class));
    }

    @Test
    @DisplayName("sendPasswordResetMail — le lien de reset est dans le contenu HTML")
    void sendPasswordResetMail_shouldContainResetLink() {
        emailService.sendPasswordResetMail("user@kupanga.com", "TOKEN_XYZ");

        ArgumentCaptor<BrevoEmail> captor = ArgumentCaptor.forClass(BrevoEmail.class);
        verify(brevoClient).send(captor.capture());

        assertThat(captor.getValue().htmlContent()).contains("TOKEN_XYZ");
    }

    @Test
    @DisplayName("sendPasswordResetMail — propage la RuntimeException du client Brevo")
    void sendPasswordResetMail_shouldPropagateException() {
        doThrow(new RuntimeException("Mail KO")).when(brevoClient).send(any());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> emailService.sendPasswordResetMail("user@kupanga.com", "token"));

        assertThat(ex.getMessage()).isEqualTo("Mail KO");
    }

    @Test
    @DisplayName("sendPasswordUpdatedConfirmation — appelle BrevoEmailClient.send() une fois")
    void sendPasswordUpdatedConfirmation_shouldCallBrevoClientOnce() {
        emailService.sendPasswordUpdatedConfirmation("user@kupanga.com");

        verify(brevoClient, times(1)).send(any(BrevoEmail.class));
    }

    @Test
    @DisplayName("sendPasswordUpdatedConfirmation — propage la RuntimeException du client Brevo")
    void sendPasswordUpdatedConfirmation_shouldPropagateException() {
        doThrow(new RuntimeException("Mail KO")).when(brevoClient).send(any());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> emailService.sendPasswordUpdatedConfirmation("user@kupanga.com"));

        assertThat(ex.getMessage()).isEqualTo("Mail KO");
    }
}
