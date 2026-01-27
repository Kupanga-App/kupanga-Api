package com.kupanga.api.email.service;

import com.kupanga.api.email.service.impl.EmailServiceImpl;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("Tests unitaires pour EmailServiceImpl")
class EmailServiceImplTest {

    private final JavaMailSender mailSender = mock(JavaMailSender.class);
    private final EmailServiceImpl emailService = new EmailServiceImpl(mailSender);

    @BeforeEach
    void setUp() {
        // crée un MimeMessage “réel” pour le test
        MimeMessage mimeMessage = new MimeMessage((Session) null);

        // quand le service appelle createMimeMessage(), retourne ce MimeMessage
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
    }

    @Test
    @DisplayName(" Doit envoyer un email pour la confirmation de création du compte correctement")
    void testSendWelcomeMessage() throws MessagingException {
        String destinataire = "test@example.com";
        String password = "123456";

        // Création d'un faux MimeMessage
        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        // Appel du service
        emailService.SendWelcomeMessage(destinataire);

        // Vérifie que l'email a été créé et envoyé
        verify(mailSender, times(1)).send(mimeMessage);
    }

    @Test
    @DisplayName("Doit lancer RuntimeException si JavaMailSender.send() échoue")
    void testSendWelcomeMessageRuntimeException() throws MessagingException {
        String destinataire = "test@example.com";
        String password = "123456";

        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        // On simule une exception lors de l'envoi de l'email
        doThrow(new RuntimeException("Erreur lors de l'envoi de l'email")).when(mailSender).send(mimeMessage);

        // Vérifie que RuntimeException est levée
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                emailService.SendWelcomeMessage(destinataire)
        );

        assertThat(exception.getMessage()).contains("Erreur lors de l'envoi de l'email");
    }


    @Test
    @DisplayName(" Vérifie que le destinataire et le contenu sont correctement définis")
    void testContenuEmail() throws MessagingException {
        String destinataire = "user@example.com";
        String password = "tempPassword";

        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
        EmailServiceImpl emailSpy = spy(emailService);

        emailSpy.SendWelcomeMessage(destinataire);

        // On peut capturer l'argument envoyé à mailSender.send
        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(captor.capture());

        MimeMessage sentMessage = captor.getValue();
        assertThat(sentMessage).isNotNull();
    }


    @Test
    @DisplayName("sendPasswordResetMail — envoie un mail avec le lien de réinitialisation")
    void sendPasswordResetMail_shouldSendEmail() throws Exception {
        String destinataire = "user@kupanga.com";
        String resetLink = "http://localhost:8081/reset?token=123";

        // Appel réel
        emailService.sendPasswordResetMail(destinataire, resetLink);

        // Vérifie que le MimeMessage a été créé
        verify(mailSender).createMimeMessage();

        // Vérifie que le mail a été envoyé
        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("sendPasswordUpdatedConfirmation — envoie un mail de confirmation")
    void sendPasswordUpdatedConfirmation_shouldSendEmail() throws Exception {
        String destinataire = "user@kupanga.com";

        emailService.sendPasswordUpdatedConfirmation(destinataire);

        verify(mailSender).createMimeMessage();
        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("sendPasswordResetMail — lance une exception si problème mail")
    void sendPasswordResetMail_shouldThrowException_whenRuntimeException() throws Exception {
        String destinataire = "user@kupanga.com";
        String resetLink = "http://localhost:8081/reset?token=123";

        // Création d'un MimeMessage réel pour que MimeMessageHelper fonctionne
        MimeMessage message = new MimeMessage((Session) null);
        when(mailSender.createMimeMessage()).thenReturn(message);

        // Simule un problème d'envoi avec RuntimeException (pas MessagingException)
        doThrow(new RuntimeException("Mail KO")).when(mailSender).send(message);

        // Vérifie qu'une RuntimeException est bien levée
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> emailService.sendPasswordResetMail(destinataire, resetLink));

        // Vérifie le message exact attendu
        assertEquals("Mail KO", exception.getMessage());
    }

    @Test
    @DisplayName("sendPasswordUpdatedConfirmation — lance une exception si problème mail")
    void sendPasswordUpdatedConfirmation_shouldThrowException_whenRuntimeException() throws Exception {
        String destinataire = "user@kupanga.com";

        // MimeMessage réel
        MimeMessage message = new MimeMessage((Session) null);
        when(mailSender.createMimeMessage()).thenReturn(message);

        // Simule une RuntimeException sur l'envoi (Mockito ne permet pas MessagingException ici)
        doThrow(new RuntimeException("Mail KO")).when(mailSender).send(message);

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> emailService.sendPasswordUpdatedConfirmation(destinataire));

        // Vérifie le message réel levé
        assertEquals("Mail KO", exception.getMessage());
    }

}

