package com.kupanga.api.email.envoi;

import com.kupanga.api.email.dto.EmailSortant;
import com.kupanga.api.email.dto.EmailSortant.PieceJointe;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.angus.mail.smtp.SMTPAddressFailedException;
import org.eclipse.angus.mail.smtp.SMTPSendFailedException;
import org.eclipse.angus.mail.smtp.SMTPSenderFailedException;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailException;
import org.springframework.mail.MailParseException;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.UUID;

/**
 * M1 : envoi par n'importe quel serveur SMTP ({@code spring.mail.*} : hôte, port, identifiant, mot de passe,
 * STARTTLS ou SSL). Message MIME en UTF-8 : HTML et texte brut en alternative, pièces jointes, {@code Message-ID} sur
 * le domaine de l'expéditeur, {@code Reply-To} facultatif.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SmtpExpediteurEmail implements ExpediteurEmail {

    /** Refus d'authentification : erreur de configuration, réessayer une fois corrigée (pas un refus définitif). */
    private static final Set<Integer> CODES_AUTHENTIFICATION = Set.of(530, 534, 535, 538);

    /** Code étendu 5.7.x (politique, relais, expéditeur non autorisé) : erreur de configuration, passagère. */
    private static final java.util.regex.Pattern REFUS_DE_CONFIGURATION = java.util.regex.Pattern.compile("\\b5\\.7\\.\\d{1,3}\\b");

    private final JavaMailSender mailSender;
    private final EmailProperties proprietes;

    @Override
    public void envoyer(EmailSortant email) throws EchecEnvoiEmail {
        MimeMessage message;
        try {
            message = preparer(email);
        } catch (MessagingException | UnsupportedEncodingException | MailException e) {
            throw new EchecEnvoiEmail("Message invalide : " + e.getClass().getSimpleName(), true, null, e);
        }
        try {
            mailSender.send(message);
        } catch (MailException e) {
            throw classer(e);
        }
    }

    MimeMessage preparer(EmailSortant email) throws MessagingException, UnsupportedEncodingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper aide = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
        aide.setFrom(proprietes.expediteur(), proprietes.nomExpediteur());
        aide.setTo(email.destinataire());
        if (proprietes.repondreA() != null && !proprietes.repondreA().isBlank()) {
            aide.setReplyTo(proprietes.repondreA());
        }
        aide.setSubject(email.sujet());
        aide.setText(email.texte(), email.html());
        for (PieceJointe piece : email.piecesJointes()) {
            aide.addAttachment(piece.nomFichier(), new ByteArrayResource(piece.contenu()), piece.typeMime());
        }
        // Gardé par JavaMailSenderImpl à l'envoi ; sur notre domaine plutôt que le nom d'hôte du conteneur
        message.setHeader("Message-ID", "<" + UUID.randomUUID() + "@" + proprietes.domaine() + ">");
        return message;
    }

    /**
     * Seul le refus définitif (5xx) du <b>destinataire</b> est définitif (adresse inexistante). Tout le reste est
     * passager (nouvel essai, M2) : réseau, 4xx, authentification, et aussi les 5xx sur l'expéditeur ou le message
     * (revue M2) : expéditeur non validé chez le fournisseur, quota dépassé… sont des erreurs de configuration ou
     * de volume qui se corrigent, et ne doivent pas faire échouer d'un coup tous les e-mails critiques.
     */
    static EchecEnvoiEmail classer(MailException e) {
        if (e instanceof MailParseException || e instanceof MailPreparationException) {
            return new EchecEnvoiEmail("Message invalide : " + e.getClass().getSimpleName(), true, null, e);
        }
        if (e instanceof MailAuthenticationException) {
            return new EchecEnvoiEmail("Authentification SMTP refusée", false, null, e);
        }
        Integer code = codeSmtp(e);
        boolean refusDestinataire = destinataireRefuse(e);
        boolean definitif = refusDestinataire && code != null && code >= 500 && !CODES_AUTHENTIFICATION.contains(code);
        String message = code == null ? "Serveur SMTP injoignable : " + racine(e)
                : (refusDestinataire ? "Destinataire refusé, SMTP " : "Refus SMTP ") + code;
        return new EchecEnvoiEmail(message, definitif, code, e);
    }

    private static boolean destinataireRefuse(MailException e) {
        if (e instanceof MailSendException envoi) {
            for (Exception echec : envoi.getMessageExceptions()) {
                if (contientRefusDestinataire(echec)) return true;
            }
        }
        return contientRefusDestinataire(e);
    }

    private static boolean contientRefusDestinataire(Throwable t) {
        for (Throwable c = t; c != null; c = c.getCause()) {
            // 5.7.x (relais non autorisé, expéditeur refusé) au RCPT : configuration, pas une adresse inexistante
            if (c instanceof SMTPAddressFailedException a) return !REFUS_DE_CONFIGURATION.matcher(String.valueOf(a.getMessage())).find();
            if (c instanceof MessagingException m && m.getNextException() != null && m.getNextException() != c
                    && contientRefusDestinataire(m.getNextException())) {
                return true;
            }
        }
        return false;
    }

    private static Integer codeSmtp(MailException e) {
        if (e instanceof MailSendException envoi) {
            for (Exception echec : envoi.getMessageExceptions()) {
                Integer code = codeDans(echec);
                if (code != null) return code;
            }
        }
        return codeDans(e);
    }

    private static Integer codeDans(Throwable t) {
        for (Throwable c = t; c != null; c = c.getCause()) {
            if (c instanceof SMTPAddressFailedException a) return a.getReturnCode();
            if (c instanceof SMTPSenderFailedException s) return s.getReturnCode();
            if (c instanceof SMTPSendFailedException f) return f.getReturnCode();
            if (c instanceof MessagingException m && m.getNextException() != null && m.getNextException() != c) {
                Integer code = codeDans(m.getNextException());
                if (code != null) return code;
            }
        }
        return null;
    }

    private static String racine(Throwable t) {
        Throwable c = t;
        while (c.getCause() != null && c.getCause() != c) c = c.getCause();
        return c.getClass().getSimpleName();
    }
}
