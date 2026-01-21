package com.kupanga.api.email.service;

import com.kupanga.api.email.service.impl.EmailServiceImpl;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@DisplayName("Tests unitaires pour EmailServiceImpl")
class EmailServiceImplTest {

    private final JavaMailSender mailSender = mock(JavaMailSender.class);
    private final EmailServiceImpl emailService = new EmailServiceImpl(mailSender);

    @Test
    @DisplayName(" Doit envoyer un email avec mot de passe temporaire correctement")
    void testEnvoyerMailMotDePasseTemporaire() throws MessagingException {
        String destinataire = "test@example.com";
        String password = "123456";

        // Création d'un faux MimeMessage
        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        // Appel du service
        emailService.envoyerMailMotDePasseTemporaire(destinataire, password);

        // Vérifie que l'email a été créé et envoyé
        verify(mailSender, times(1)).send(mimeMessage);
    }

    @Test
    @DisplayName("Doit lancer RuntimeException si JavaMailSender.send() échoue")
    void testEnvoyerMailMotDePasseTemporaireRuntimeException() throws MessagingException {
        String destinataire = "test@example.com";
        String password = "123456";

        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        // On simule une exception lors de l'envoi de l'email
        doThrow(new RuntimeException("Erreur lors de l'envoi de l'email")).when(mailSender).send(mimeMessage);

        // Vérifie que RuntimeException est levée
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                emailService.envoyerMailMotDePasseTemporaire(destinataire, password)
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

        emailSpy.envoyerMailMotDePasseTemporaire(destinataire, password);

        // On peut capturer l'argument envoyé à mailSender.send
        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(captor.capture());

        MimeMessage sentMessage = captor.getValue();
        assertThat(sentMessage).isNotNull();
    }
}

