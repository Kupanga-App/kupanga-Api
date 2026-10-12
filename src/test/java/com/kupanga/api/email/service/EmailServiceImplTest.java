package com.kupanga.api.email.service;

import com.kupanga.api.juridiction.Pays;
import com.kupanga.api.email.client.BrevoEmailClient;
import com.kupanga.api.email.dto.BrevoEmail;
import com.kupanga.api.email.event.EmailAEnvoyer;
import com.kupanga.api.email.event.EmailEnvoiListener;
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
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;
import java.math.BigDecimal;

@DisplayName("Tests unitaires pour EmailServiceImpl")
class EmailServiceImplTest {

    /** J3 : montants formatés comme en France (registre bouchon). */
    private static com.kupanga.api.juridiction.JuridictionRegistry registreFr() {
        com.kupanga.api.juridiction.JuridictionRegistry registre =
                org.mockito.Mockito.mock(com.kupanga.api.juridiction.JuridictionRegistry.class);
        org.mockito.Mockito.when(registre.formatMontant(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenAnswer(i -> new com.kupanga.api.juridiction.FormatMontant(java.util.Locale.FRANCE,
                        i.getArgument(1) == null ? com.kupanga.api.juridiction.Devise.EUR : i.getArgument(1)));
        return registre;
    }

    private final BrevoEmailClient brevoClient = mock(BrevoEmailClient.class);
    private final MinioService minioService = mock(MinioService.class);

    // B11 : l'envoi passe par un événement ; ici l'écouteur réel est appelé tout de suite (pas de transaction)
    private final EmailEnvoiListener listener = new EmailEnvoiListener(brevoClient, minioService);

    private final EmailServiceImpl emailService = new EmailServiceImpl(
            evenement -> listener.envoyer((EmailAEnvoyer) evenement),
            "noreplydevback@gmail.com",
            "Kupanga",
            "http://localhost:4200/auth/reset-password?token=",
            "http://localhost:4200/auth/login",
            "http://localhost:4200/"
    , registreFr());

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
                .loyerMensuel(new BigDecimal("800.0")).chargesMensuelles(new BigDecimal("50.0")).montantTotal(new BigDecimal("850.0"))
                .bien(Bien.builder().pays(Pays.FR).adresse("1 rue A").codePostal("44000").ville("Nantes").build())
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
    @DisplayName("sendWelcomeMessage — une erreur Brevo est journalisée, jamais propagée à l'appelant (B11 : envoi après commit)")
    void sendWelcomeMessage_erreurBrevo_nonPropagee() {
        doThrow(new RuntimeException("Brevo KO")).when(brevoClient).send(any());

        assertDoesNotThrow(() -> emailService.sendWelcomeMessage("test@example.com", "Alice"));

        verify(brevoClient).send(any(BrevoEmail.class));
    }

    @Test
    @DisplayName("envoyerQuittance — PDF illisible dans MinIO → e-mail envoyé quand même, sans pièce jointe")
    void envoyerQuittance_pdfIllisible_emailSansPieceJointe() {
        when(minioService.telecharger(QUITTANCE_BUCKET, "cle-quittance.pdf")).thenThrow(new IllegalStateException("MinIO KO"));

        Quittance quittance = Quittance.builder()
                .id(3L).mois("janvier").annee(2026)
                .loyerMensuel(new BigDecimal("800.0")).chargesMensuelles(new BigDecimal("50.0")).montantTotal(new BigDecimal("850.0"))
                .bien(Bien.builder().pays(Pays.FR).adresse("1 rue A").codePostal("44000").ville("Nantes").build())
                .locataire(User.builder().firstName("Alice").lastName("Martin").mail("alice@example.com").build())
                .clePdf("cle-quittance.pdf")
                .build();

        assertDoesNotThrow(() -> emailService.envoyerQuittance(quittance));

        ArgumentCaptor<BrevoEmail> captor = ArgumentCaptor.forClass(BrevoEmail.class);
        verify(brevoClient).send(captor.capture());
        assertThat(captor.getValue().attachment()).isNull();
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
    @DisplayName("sendPasswordResetMail — une erreur Brevo est journalisée, jamais propagée à l'appelant (B11 : envoi après commit)")
    void sendPasswordResetMail_erreurBrevo_nonPropagee() {
        doThrow(new RuntimeException("Mail KO")).when(brevoClient).send(any());

        assertDoesNotThrow(() -> emailService.sendPasswordResetMail("user@kupanga.com", "token"));

        verify(brevoClient).send(any(BrevoEmail.class));
    }

    @Test
    @DisplayName("sendPasswordUpdatedConfirmation — appelle BrevoEmailClient.send() une fois")
    void sendPasswordUpdatedConfirmation_shouldCallBrevoClientOnce() {
        emailService.sendPasswordUpdatedConfirmation("user@kupanga.com");

        verify(brevoClient, times(1)).send(any(BrevoEmail.class));
    }

    @Test
    @DisplayName("sendPasswordUpdatedConfirmation — une erreur Brevo est journalisée, jamais propagée à l'appelant (B11 : envoi après commit)")
    void sendPasswordUpdatedConfirmation_erreurBrevo_nonPropagee() {
        doThrow(new RuntimeException("Mail KO")).when(brevoClient).send(any());

        assertDoesNotThrow(() -> emailService.sendPasswordUpdatedConfirmation("user@kupanga.com"));

        verify(brevoClient).send(any(BrevoEmail.class));
    }
}
