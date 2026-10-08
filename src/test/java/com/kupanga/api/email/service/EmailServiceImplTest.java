package com.kupanga.api.email.service;

import com.kupanga.api.email.client.BrevoEmailClient;
import com.kupanga.api.email.dto.BrevoEmail;
import com.kupanga.api.email.service.impl.EmailServiceImpl;
import com.kupanga.api.immobilier.entity.Bien;
import com.kupanga.api.immobilier.entity.Quittance;
import com.kupanga.api.minio.service.MinioService;
import com.kupanga.api.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Base64;

import static com.kupanga.api.minio.constant.MinioConstant.QUITTANCE_BUCKET;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@DisplayName("Tests unitaires pour EmailServiceImpl")
class EmailServiceImplTest {

    private final BrevoEmailClient brevoClient = mock(BrevoEmailClient.class);
    private final MinioService minioService = mock(MinioService.class);

    private final EmailServiceImpl emailService = new EmailServiceImpl(
            brevoClient,
            minioService,
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
    @DisplayName("sendWelcomeMessage — le prénom est échappé dans le HTML (pas d'injection HTML)")
    void sendWelcomeMessage_shouldEscapeHtml() {
        emailService.sendWelcomeMessage("test@example.com", "<a href=\"http://pirate\">Alice</a>");

        ArgumentCaptor<BrevoEmail> captor = ArgumentCaptor.forClass(BrevoEmail.class);
        verify(brevoClient).send(captor.capture());

        assertThat(captor.getValue().htmlContent())
                .doesNotContain("<a href=\"http://pirate\">")
                .contains("&lt;a href=&quot;http://pirate&quot;&gt;Alice&lt;/a&gt;");
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
    @DisplayName("envoyerQuittance — PDF joint en base64 depuis le bucket privé, jamais par URL (P0-7)")
    void envoyerQuittance_shouldAttachPdfAsBase64() {
        byte[] pdf = "%PDF-1.4 test".getBytes();
        when(minioService.telecharger(QUITTANCE_BUCKET, "cle-quittance.pdf")).thenReturn(pdf);

        Quittance quittance = Quittance.builder()
                .id(3L).mois("janvier").annee(2026)
                .loyerMensuel(800.0).chargesMensuelles(50.0).montantTotal(850.0)
                .bien(Bien.builder().adresse("1 rue A").codePostal("44000").ville("Nantes").build())
                .locataire(User.builder().firstName("Alice").lastName("Martin").mail("alice@example.com").build())
                .clePdf("cle-quittance.pdf")
                .build();

        emailService.envoyerQuittance(quittance);

        ArgumentCaptor<BrevoEmail> captor = ArgumentCaptor.forClass(BrevoEmail.class);
        verify(brevoClient).send(captor.capture());
        BrevoEmail.Attachment pj = captor.getValue().attachment().get(0);
        assertThat(pj.content()).isEqualTo(Base64.getEncoder().encodeToString(pdf));
        assertThat(pj.name()).endsWith(".pdf");
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
