package com.kupanga.api.email.envoi;

import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetupTest;
import com.kupanga.api.email.dto.EmailSortant;
import com.kupanga.api.email.dto.EmailSortant.PieceJointe;
import jakarta.mail.Address;
import jakarta.mail.BodyPart;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.eclipse.angus.mail.smtp.SMTPAddressFailedException;
import org.eclipse.angus.mail.smtp.SMTPSendFailedException;
import org.eclipse.angus.mail.smtp.SMTPSenderFailedException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("M1 : envoi par un vrai serveur SMTP (GreenMail)")
class SmtpExpediteurEmailTest {

    @RegisterExtension
    static GreenMailExtension smtp = new GreenMailExtension(ServerSetupTest.SMTP);

    private static final EmailProperties PROPRIETES =
            new EmailProperties("noreply@kupanga.test", "Kupanga", "contact@kupanga.test");

    private SmtpExpediteurEmail expediteur(int port) {
        JavaMailSenderImpl envoi = new JavaMailSenderImpl();
        envoi.setHost("localhost");
        envoi.setPort(port);
        envoi.setDefaultEncoding("UTF-8");
        Properties p = new Properties();
        p.put("mail.smtp.connectiontimeout", "2000");
        p.put("mail.smtp.timeout", "2000");
        envoi.setJavaMailProperties(p);
        return new SmtpExpediteurEmail(envoi, PROPRIETES);
    }

    @Test
    @DisplayName("Message UTF-8 : expéditeur et Reply-To configurés, HTML + texte brut, accents, Message-ID sur notre domaine")
    void envoi_complet() throws Exception {
        String html = "<p>Bonjour Grâce, <a href=\"https://kupanga.test/auth/verifier-email?token=t\">confirmer</a></p>";

        expediteur(smtp.getSmtp().getPort()).envoyer(EmailSortant.html("grace@exemple.cd", "Été à Kinshasa : bienvenue !", html));

        MimeMessage recu = smtp.getReceivedMessages()[0];
        assertThat(recu.getSubject()).isEqualTo("Été à Kinshasa : bienvenue !");
        assertThat(((InternetAddress) recu.getFrom()[0]).getAddress()).isEqualTo("noreply@kupanga.test");
        assertThat(((InternetAddress) recu.getFrom()[0]).getPersonal()).isEqualTo("Kupanga");
        assertThat(adresses(recu.getReplyTo())).containsExactly("contact@kupanga.test");
        assertThat(adresses(recu.getAllRecipients())).containsExactly("grace@exemple.cd");
        assertThat(recu.getMessageID()).matches("<[0-9a-f-]{36}@kupanga\\.test>");

        List<String> types = new ArrayList<>();
        List<String> contenus = new ArrayList<>();
        parcourir(recu, types, contenus);
        assertThat(types).anyMatch(t -> t.startsWith("text/plain")).anyMatch(t -> t.startsWith("text/html"));
        assertThat(contenus).anyMatch(c -> c.contains("Bonjour Grâce, confirmer : https://kupanga.test/auth/verifier-email?token=t"));
        assertThat(contenus).anyMatch(c -> c.contains("<a href="));
    }

    @Test
    @DisplayName("Pièce jointe PDF transmise avec son nom et son type")
    void pieceJointe() throws Exception {
        byte[] pdf = "%PDF-1.4 quittance".getBytes();
        EmailSortant email = EmailSortant.html("loc@exemple.fr", "Quittance", "<p>Votre quittance</p>")
                .avecPiecesJointes(List.of(new PieceJointe("Quittance_1.pdf", pdf, "application/pdf")));

        expediteur(smtp.getSmtp().getPort()).envoyer(email);

        Multipart corps = (Multipart) smtp.getReceivedMessages()[0].getContent();
        BodyPart piece = null;
        for (int i = 0; i < corps.getCount(); i++) {
            if (Part.ATTACHMENT.equalsIgnoreCase(corps.getBodyPart(i).getDisposition())) piece = corps.getBodyPart(i);
        }
        assertThat(piece).isNotNull();
        assertThat(piece.getFileName()).isEqualTo("Quittance_1.pdf");
        assertThat(piece.getContentType()).startsWith("application/pdf");
        assertThat(piece.getInputStream().readAllBytes()).isEqualTo(pdf);
    }

    @Test
    @DisplayName("Serveur SMTP injoignable → échec passager (nouvel essai), sans adresse ni sujet dans le message")
    void serveurInjoignable_passager() {
        assertThatThrownBy(() -> expediteur(1).envoyer(EmailSortant.html("secret@exemple.fr", "Sujet privé", "<p>x</p>")))
                .isInstanceOfSatisfying(EchecEnvoiEmail.class, e -> {
                    assertThat(e.isDefinitif()).isFalse();
                    assertThat(e.getMessage()).doesNotContain("secret@exemple.fr").doesNotContain("Sujet privé");
                });
    }

    @Test
    @DisplayName("Classement des refus : 550 définitif, 451 passager, authentification passagère")
    void classement() {
        MailSendException refus550 = new MailSendException(Map.of(new Object(),
                new SMTPAddressFailedException(new InternetAddress(), "RCPT", 550, "Utilisateur inconnu")));
        MailSendException refus451 = new MailSendException(Map.of(new Object(),
                new SMTPAddressFailedException(new InternetAddress(), "RCPT", 451, "Réessayez plus tard")));

        EchecEnvoiEmail definitif = SmtpExpediteurEmail.classer(refus550);
        assertThat(definitif.isDefinitif()).isTrue();
        assertThat(definitif.getCodeSmtp()).isEqualTo(550);
        assertThat(definitif.getMessage()).isEqualTo("Destinataire refusé, SMTP 550");
        assertThat(SmtpExpediteurEmail.classer(refus451).isDefinitif()).isFalse();
        assertThat(SmtpExpediteurEmail.classer(new MailAuthenticationException("535")).isDefinitif()).isFalse();
    }

    @Test
    @DisplayName("Revue M2 : 5xx sur l'expéditeur (non validé, quota) ou sur le message → passager, pas d'échec définitif")
    void refusExpediteur_passager() {
        MailSendException expediteurRefuse = new MailSendException(Map.of(new Object(),
                new SMTPSenderFailedException(new InternetAddress(), "MAIL FROM", 550, "Sender not allowed")));
        MailSendException quota = new MailSendException(Map.of(new Object(),
                new SMTPSendFailedException("DATA", 554, "Daily sending quota exceeded", null, null, null, null)));

        // Refus au RCPT pour une raison de configuration (relais non autorisé) : passager aussi
        MailSendException relaisRefuse = new MailSendException(Map.of(new Object(),
                new SMTPAddressFailedException(new InternetAddress(), "RCPT", 554, "554 5.7.1 Relay access denied")));
        assertThat(SmtpExpediteurEmail.classer(relaisRefuse).isDefinitif()).isFalse();

        EchecEnvoiEmail echec = SmtpExpediteurEmail.classer(expediteurRefuse);
        assertThat(echec.isDefinitif()).isFalse();
        assertThat(echec.getMessage()).isEqualTo("Refus SMTP 550");
        assertThat(SmtpExpediteurEmail.classer(quota).isDefinitif()).isFalse();
    }

    @Test
    @DisplayName("Sujet contenant un retour à la ligne (prénom piégé) → aucun en-tête injecté")
    void sujetAvecRetourALaLigne_aucunEnTeteInjecte() throws Exception {
        expediteur(smtp.getSmtp().getPort()).envoyer(
                EmailSortant.html("a@exemple.fr", "Bonjour X\r\nBcc: espion@pirate.test", "<p>x</p>"));

        MimeMessage recu = smtp.getReceivedMessages()[0];
        assertThat(adresses(recu.getAllRecipients())).containsExactly("a@exemple.fr");
        assertThat(recu.getHeader("Bcc")).isNull();
        assertThat(smtp.getReceivedMessages()).hasSize(1);
    }

    @Test
    @DisplayName("Sans Reply-To configuré : aucun en-tête Reply-To distinct de l'expéditeur")
    void sansReplyTo() throws Exception {
        JavaMailSenderImpl envoi = new JavaMailSenderImpl();
        envoi.setHost("localhost");
        envoi.setPort(smtp.getSmtp().getPort());
        new SmtpExpediteurEmail(envoi, new EmailProperties("noreply@kupanga.test", "Kupanga", null))
                .envoyer(EmailSortant.html("a@exemple.fr", "S", "<p>x</p>"));

        assertThat(GreenMailUtil.getHeaders(smtp.getReceivedMessages()[0])).doesNotContain("Reply-To:");
    }

    private static List<String> adresses(Address[] adresses) {
        List<String> liste = new ArrayList<>();
        for (Address a : adresses) liste.add(((InternetAddress) a).getAddress());
        return liste;
    }

    private static void parcourir(Part partie, List<String> types, List<String> contenus) throws Exception {
        Object contenu = partie.getContent();
        if (contenu instanceof Multipart multi) {
            for (int i = 0; i < multi.getCount(); i++) parcourir(multi.getBodyPart(i), types, contenus);
        } else {
            types.add(partie.getContentType().toLowerCase());
            if (contenu instanceof String texte) contenus.add(texte);
        }
    }
}
